package com.vztechcore.vzgallery.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.vztechcore.vzgallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Vault real:
 *   - Files moved to filesDir/locked/ (internal private storage)
 *   - Encrypted with AES-256-GCM via Android Keystore (key in TEE/StrongBox)
 *   - Removed from MediaStore (invisible to any other app)
 *   - Restore: write back to public folder + rescan
 *
 * Password hash (UI authentication only, does NOT encrypt files):
 *   SHA-256(salt + password), salt = 16 bytes SecureRandom
 */
object LockRepository {

    private const val SALT_SIZE = 16
    private const val KEYSTORE_ALIAS = "vzgallery_vault_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_SIZE = 12
    private const val LOCKED_DIR = "locked"
    private const val CACHE_DIR = "vault_decrypted"

    // ====================== PASSWORD HASH ======================

    fun generateSalt(): String {
        val bytes = ByteArray(SALT_SIZE)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((salt + password).toByteArray(Charsets.UTF_8)).toHex()
    }

    fun verifyPassword(input: String, salt: String, expectedHash: String): Boolean {
        val actual = hashPassword(input, salt)
        if (actual.length != expectedHash.length) return false
        var result = 0
        for (i in actual.indices) result = result or (actual[i].code xor expectedHash[i].code)
        return result == 0
    }

    // ====================== AES + KEYSTORE ======================

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encryptBytes(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decryptBytes(data: ByteArray): ByteArray {
        require(data.size > GCM_IV_SIZE) { "Corrupted vault file" }
        val iv = data.copyOfRange(0, GCM_IV_SIZE)
        val cipherText = data.copyOfRange(GCM_IV_SIZE, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    // ====================== DIRECTORIES ======================

    private fun lockedDir(context: Context): File {
        val dir = File(context.filesDir, LOCKED_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ====================== LOCK (move to vault) ======================

    suspend fun lockFile(context: Context, item: MediaItem): Result<LockedFile> =
        withContext(Dispatchers.IO) {
            try {
                val id = UUID.randomUUID().toString()
                val encFile = File(lockedDir(context), "$id.enc")
                val metaFile = File(lockedDir(context), "$id.json")

                val input = context.contentResolver.openInputStream(item.uri)
                    ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo"))

                val plain = input.use { it.readBytes() }
                if (plain.isEmpty()) {
                    return@withContext Result.failure(Exception("Arquivo vazio"))
                }

                encFile.writeBytes(encryptBytes(plain))

                // Preserve dates BEFORE deleting from MediaStore
                val dateTaken = queryDateTaken(context, item.uri)
                val dateAdded = queryDateAdded(context, item.uri)
                val dateModified = queryDateModified(context, item.uri)

                val meta = LockedFile(
                    id = id,
                    originalName = item.nome,
                    originalPath = item.relativePath,
                    isVideo = item.isVideo,
                    originalUri = item.uri.toString(),
                    sizeBytes = plain.size.toLong(),
                    lockedAt = System.currentTimeMillis(),
                    dateTakenMs = dateTaken,
                    dateAddedSec = dateAdded,
                    dateModifiedSec = dateModified
                )
                metaFile.writeText(meta.toJson().toString())

                val deleted = try {
                    context.contentResolver.delete(item.uri, null, null) > 0
                } catch (_: Exception) { false }

                if (!deleted) {
                    encFile.delete()
                    metaFile.delete()
                    return@withContext Result.failure(
                        Exception("Não foi possível remover o arquivo original")
                    )
                }

                Result.success(meta)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ====================== RESTORE (move back to gallery) ======================

    suspend fun unlockFile(context: Context, locked: LockedFile): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val encFile = File(lockedDir(context), "${locked.id}.enc")
                val metaFile = File(lockedDir(context), "${locked.id}.json")
                if (!encFile.exists()) {
                    return@withContext Result.failure(Exception("Arquivo do cofre não encontrado"))
                }

                val decrypted = decryptBytes(encFile.readBytes())
                val restored = writeToGallery(context, locked, decrypted)
                if (!restored) {
                    return@withContext Result.failure(Exception("Não foi possível restaurar"))
                }

                encFile.delete()
                metaFile.delete()
                File(cacheDir(context), "${locked.id}.bin").delete()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun writeToGallery(context: Context, locked: LockedFile, bytes: ByteArray): Boolean {
        return try {
            val targetDir = if (locked.isVideo) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            if (!targetDir.exists()) targetDir.mkdirs()

            var targetFile = File(targetDir, locked.originalName)
            var counter = 1
            while (targetFile.exists()) {
                val base = locked.originalName.substringBeforeLast('.')
                val ext = locked.originalName.substringAfterLast('.', "")
                targetFile = File(targetDir, "${base}_${counter++}.${ext}")
            }
            targetFile.writeBytes(bytes)

            // Preserve modified time (file system)
            locked.dateModifiedSec?.let { targetFile.setLastModified(it * 1000L) }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                null
            ) { _, uri ->
                uri?.let { patchMediaStoreDates(context, it, locked) }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun patchMediaStoreDates(context: Context, uri: Uri, locked: LockedFile) {
        try {
            val values = ContentValues().apply {
                locked.dateTakenMs?.let {
                    put(MediaStore.MediaColumns.DATE_TAKEN, it)
                }
                locked.dateModifiedSec?.let {
                    put(MediaStore.MediaColumns.DATE_MODIFIED, it)
                }
            }
            if (values.size() > 0) {
                context.contentResolver.update(uri, values, null, null)
            }
        } catch (_: Exception) {}
    }

    private fun queryDateTaken(context: Context, uri: Uri): Long? =
        queryLong(context, uri, MediaStore.MediaColumns.DATE_TAKEN)

    private fun queryDateAdded(context: Context, uri: Uri): Long? =
        queryLong(context, uri, MediaStore.MediaColumns.DATE_ADDED)

    private fun queryDateModified(context: Context, uri: Uri): Long? =
        queryLong(context, uri, MediaStore.MediaColumns.DATE_MODIFIED)

    private fun queryLong(context: Context, uri: Uri, column: String): Long? =
        try {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val v = c.getLong(0)
                    if (v > 0) v else null
                } else null
            }
        } catch (_: Exception) { null }

    // ====================== DECRYPT TO CACHE (for thumbnails / viewing) ======================

    /**
     * Decrypts a locked file to cache (if not already cached) and returns the File.
     * Returns null on error. Safe to call from any thread.
     */
    suspend fun getDecryptedFile(context: Context, locked: LockedFile): File? =
        withContext(Dispatchers.IO) {
            try {
                val cached = File(cacheDir(context), "${locked.id}.bin")
                if (cached.exists() && cached.length() > 0) return@withContext cached

                val encFile = File(lockedDir(context), "${locked.id}.enc")
                if (!encFile.exists()) return@withContext null

                val decrypted = decryptBytes(encFile.readBytes())
                cached.writeBytes(decrypted)
                cached
            } catch (_: Exception) { null }
        }

    suspend fun getDecryptedUri(context: Context, locked: LockedFile): Uri? {
        val file = getDecryptedFile(context, locked) ?: return null
        return Uri.fromFile(file)
    }

    // ====================== DELETE FROM VAULT ======================

    suspend fun deleteLocked(context: Context, locked: LockedFile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                File(lockedDir(context), "${locked.id}.enc").delete()
                File(lockedDir(context), "${locked.id}.json").delete()
                File(cacheDir(context), "${locked.id}.bin").delete()
                true
            } catch (_: Exception) { false }
        }

    // ====================== LIST / INFO ======================

    suspend fun listLocked(context: Context): List<LockedFile> =
        withContext(Dispatchers.IO) {
            lockedDir(context).listFiles { f -> f.extension == "json" }
                ?.mapNotNull { metaFile ->
                    runCatching {
                        LockedFile.fromJson(JSONObject(metaFile.readText()))
                    }.getOrNull()
                }
                ?.sortedByDescending { it.lockedAt }
                ?: emptyList()
        }

    suspend fun totalLockedSize(context: Context): Long =
        withContext(Dispatchers.IO) {
            lockedDir(context).listFiles { f -> f.extension == "enc" }
                ?.sumOf { it.length() }
                ?: 0L
        }

    suspend fun wipeVault(context: Context) = withContext(Dispatchers.IO) {
        lockedDir(context).listFiles()?.forEach { it.delete() }
        cacheDir(context).listFiles()?.forEach { it.delete() }
    }

    // ====================== HELPERS ======================

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    data class LockedFile(
        val id: String,
        val originalName: String,
        val originalPath: String,
        val isVideo: Boolean,
        val originalUri: String,
        val sizeBytes: Long,
        val lockedAt: Long,
        val dateTakenMs: Long? = null,
        val dateAddedSec: Long? = null,
        val dateModifiedSec: Long? = null
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("originalName", originalName)
            put("originalPath", originalPath)
            put("isVideo", isVideo)
            put("originalUri", originalUri)
            put("sizeBytes", sizeBytes)
            put("lockedAt", lockedAt)
            dateTakenMs?.let { put("dateTakenMs", it) }
            dateAddedSec?.let { put("dateAddedSec", it) }
            dateModifiedSec?.let { put("dateModifiedSec", it) }
        }

        companion object {
            fun fromJson(json: JSONObject): LockedFile = LockedFile(
                id = json.getString("id"),
                originalName = json.getString("originalName"),
                originalPath = json.optString("originalPath", ""),
                isVideo = json.optBoolean("isVideo", false),
                originalUri = json.optString("originalUri", ""),
                sizeBytes = json.optLong("sizeBytes", 0L),
                lockedAt = json.optLong("lockedAt", 0L),
                dateTakenMs = if (json.has("dateTakenMs")) json.getLong("dateTakenMs") else null,
                dateAddedSec = if (json.has("dateAddedSec")) json.getLong("dateAddedSec") else null,
                dateModifiedSec = if (json.has("dateModifiedSec")) json.getLong("dateModifiedSec") else null
            )
        }
    }
}