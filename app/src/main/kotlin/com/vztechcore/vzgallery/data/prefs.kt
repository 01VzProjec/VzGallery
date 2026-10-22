package com.vztechcore.vzgallery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("vzgallery_prefs")

class Prefs(private val context: Context) {

    private companion object {
        val KEY_WELCOME_SHOWN      = booleanPreferencesKey("welcome_shown")
        val KEY_PERMISSION_GRANTED = booleanPreferencesKey("permission_granted")
        val KEY_SHOW_SVG           = booleanPreferencesKey("show_svg")
        val KEY_HIDDEN_ITEMS       = stringSetPreferencesKey("hidden_items")
        val KEY_HIDDEN_AT          = stringSetPreferencesKey("hidden_at")
        val KEY_HIDDEN_FOLDERS     = stringSetPreferencesKey("hidden_folders")
        val KEY_FAVORITES          = stringSetPreferencesKey("favorites")
        val KEY_THEME_MODE         = stringPreferencesKey("theme_mode")
        val KEY_GRID_SIZE          = stringPreferencesKey("grid_size")
        val KEY_AUTO_RELOAD        = booleanPreferencesKey("auto_reload")
        val KEY_SKIP_OPENING_MS    = longPreferencesKey("skip_opening_ms")
        val KEY_ANIMATION_MODE     = stringPreferencesKey("animation_mode")
        val KEY_SHOW_NAME          = booleanPreferencesKey("show_name")
        val KEY_SHOW_FAVORITE_BADGE = booleanPreferencesKey("show_favorite_badge")
        val KEY_FAVORITE_ALBUMS    = stringSetPreferencesKey("favorite_albums")
        val KEY_PREFS_VERSION      = intPreferencesKey("prefs_version")

        // ===== Área Trancada =====
        val KEY_LOCK_HASH          = stringPreferencesKey("lock_hash")
        val KEY_LOCK_SALT          = stringPreferencesKey("lock_salt")
        val KEY_LOCK_TYPE          = stringPreferencesKey("lock_type")
        val KEY_LOCK_BIOMETRIC     = booleanPreferencesKey("lock_biometric_enabled")

        val KEY_ALBUM_HIDE_DATE_PREFIX = "album_hide_date_"
        val KEY_ALBUM_ORDER_PREFIX     = "album_order_"
        val KEY_ALBUM_COVER_PREFIX     = "album_cover_"

        fun orderKey(album: AlbumType)    = stringPreferencesKey("order_${album.name}")
        fun hideDateKey(album: AlbumType) = booleanPreferencesKey("hide_date_${album.name}")
        fun coverKey(album: AlbumType)    = stringPreferencesKey("cover_${album.name}")

        fun albumOrderKey(path: String)    = stringPreferencesKey("$KEY_ALBUM_ORDER_PREFIX$path")
        fun albumHideDateKey(path: String) = booleanPreferencesKey("$KEY_ALBUM_HIDE_DATE_PREFIX$path")
        fun albumCoverKey(path: String)    = stringPreferencesKey("$KEY_ALBUM_COVER_PREFIX$path")
    }

    // ===== Global =====
    val welcomeShown: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_WELCOME_SHOWN] ?: false }

    val permissionGranted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_PERMISSION_GRANTED] ?: false }

    val showSvg: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_SVG] ?: false }

    val hiddenItems: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_HIDDEN_ITEMS] ?: emptySet() }

    val hiddenAt: Flow<Map<String, Long>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_HIDDEN_AT] ?: emptySet()
            raw.mapNotNull { entry ->
                val parts = entry.split("|", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val uri = parts[0]
                val ts = parts[1].toLongOrNull() ?: return@mapNotNull null
                uri to ts
            }.toMap()
        }

    val hiddenFolders: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_HIDDEN_FOLDERS] ?: emptySet() }

    val favorites: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_FAVORITES] ?: emptySet() }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_THEME_MODE]
            raw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    val gridSize: Flow<GridSize> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_GRID_SIZE]
            raw?.let { runCatching { GridSize.valueOf(it) }.getOrNull() }
                ?: GridSize.NORMAL
        }

    val autoReload: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_AUTO_RELOAD] ?: true }

    val skipOpeningMs: Flow<Long> = context.dataStore.data
        .map { it[KEY_SKIP_OPENING_MS] ?: 850L }

    val animationMode: Flow<AnimationMode> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_ANIMATION_MODE]
            raw?.let { runCatching { AnimationMode.valueOf(it) }.getOrNull() }
                ?: AnimationMode.ANIM_1
        }

    val showName: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_NAME] ?: false }

    val showFavoriteBadge: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_SHOW_FAVORITE_BADGE] ?: true }

    val favoriteAlbums: Flow<Set<String>> = context.dataStore.data
        .map { it[KEY_FAVORITE_ALBUMS] ?: emptySet() }

    val prefsVersion: Flow<Int> = context.dataStore.data
        .map { it[KEY_PREFS_VERSION] ?: 1 }

    // ===== Área Trancada =====
    val lockConfigured: Flow<Boolean> = context.dataStore.data
        .map { !it[KEY_LOCK_HASH].isNullOrEmpty() && !it[KEY_LOCK_SALT].isNullOrEmpty() }

    val lockType: Flow<LockType> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_LOCK_TYPE]
            raw?.let { runCatching { LockType.valueOf(it) }.getOrNull() }
                ?: LockType.PIN
        }

    val lockBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_LOCK_BIOMETRIC] ?: false }

    // ===== Global setters =====
    suspend fun setWelcomeShown(value: Boolean) {
        context.dataStore.edit { it[KEY_WELCOME_SHOWN] = value }
    }

    suspend fun setPermissionGranted(value: Boolean) {
        context.dataStore.edit { it[KEY_PERMISSION_GRANTED] = value }
    }

    suspend fun setShowSvg(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_SVG] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setGridSize(size: GridSize) {
        context.dataStore.edit { it[KEY_GRID_SIZE] = size.name }
    }

    suspend fun setAutoReload(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_RELOAD] = value }
    }

    suspend fun setSkipOpeningMs(value: Long) {
        context.dataStore.edit { it[KEY_SKIP_OPENING_MS] = value }
    }

    suspend fun setAnimationMode(mode: AnimationMode) {
        context.dataStore.edit { it[KEY_ANIMATION_MODE] = mode.name }
    }

    suspend fun setShowName(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_NAME] = value }
    }

    suspend fun setShowFavoriteBadge(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_FAVORITE_BADGE] = value }
    }

    suspend fun setPrefsVersion(value: Int) {
        context.dataStore.edit { it[KEY_PREFS_VERSION] = value }
    }

    // ===== Área Trancada setters =====
    suspend fun saveLock(type: LockType, passwordHash: String, salt: String) {
        context.dataStore.edit {
            it[KEY_LOCK_TYPE] = type.name
            it[KEY_LOCK_HASH] = passwordHash
            it[KEY_LOCK_SALT] = salt
        }
    }

    suspend fun setLockBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOCK_BIOMETRIC] = enabled }
    }

    /** Remove o lock. NÃO apaga os arquivos do cofre — chamar LockRepository.wipeVault separado. */
    suspend fun clearLock() {
        context.dataStore.edit {
            it.remove(KEY_LOCK_HASH)
            it.remove(KEY_LOCK_SALT)
            it.remove(KEY_LOCK_TYPE)
            it.remove(KEY_LOCK_BIOMETRIC)
        }
    }

    /** Verifica senha/PIN contra o hash salvo. One-shot (não é Flow). */
    suspend fun verifyLockPassword(input: String): Boolean {
        val prefs = context.dataStore.data.first()
        val hash = prefs[KEY_LOCK_HASH] ?: return false
        val salt = prefs[KEY_LOCK_SALT] ?: return false
        return LockRepository.verifyPassword(input, salt, hash)
    }

    // ===== Hidden items =====
    suspend fun hideItems(uris: Set<String>) {
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN_ITEMS] ?: emptySet()
            prefs[KEY_HIDDEN_ITEMS] = current + uris

            val currentRaw = prefs[KEY_HIDDEN_AT] ?: emptySet()
            val filtered = currentRaw.filterNot { entry ->
                val uri = entry.substringBefore("|")
                uri in uris
            }.toSet()
            val newEntries = uris.map { "$it|$now" }.toSet()
            prefs[KEY_HIDDEN_AT] = filtered + newEntries
        }
    }

    suspend fun showItems(uris: Set<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN_ITEMS] ?: emptySet()
            prefs[KEY_HIDDEN_ITEMS] = current - uris

            val currentRaw = prefs[KEY_HIDDEN_AT] ?: emptySet()
            prefs[KEY_HIDDEN_AT] = currentRaw.filterNot { entry ->
                entry.substringBefore("|") in uris
            }.toSet()
        }
    }

    // ===== Hidden folders =====
    suspend fun setHiddenFolders(paths: Set<String>) {
        context.dataStore.edit { it[KEY_HIDDEN_FOLDERS] = paths }
    }

    suspend fun toggleHiddenFolder(path: String, hide: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HIDDEN_FOLDERS] ?: emptySet()
            prefs[KEY_HIDDEN_FOLDERS] = if (hide) current + path else current - path
        }
    }

    // ===== Item favorites =====
    suspend fun toggleFavorite(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES] ?: emptySet()
            prefs[KEY_FAVORITES] = if (uri in current) current - uri else current + uri
        }
    }

    suspend fun setFavorite(uri: String, favorite: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES] ?: emptySet()
            prefs[KEY_FAVORITES] = if (favorite) current + uri else current - uri
        }
    }

    // ===== Album/pasta favorites =====
    suspend fun toggleFavoriteAlbum(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITE_ALBUMS] ?: emptySet()
            prefs[KEY_FAVORITE_ALBUMS] = if (path in current) current - path else current + path
        }
    }

    // ===== Per-album (AlbumType) =====
    fun orderMode(album: AlbumType): Flow<SortMode> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[orderKey(album)]
            raw?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
                ?: SortMode.RECENT_FIRST
        }

    fun hideDate(album: AlbumType): Flow<Boolean> = context.dataStore.data
        .map { it[hideDateKey(album)] ?: false }

    fun customCover(album: AlbumType): Flow<String?> = context.dataStore.data
        .map { it[coverKey(album)] }

    suspend fun setOrderMode(album: AlbumType, mode: SortMode) {
        context.dataStore.edit { it[orderKey(album)] = mode.name }
    }

    suspend fun setHideDate(album: AlbumType, hide: Boolean) {
        context.dataStore.edit { it[hideDateKey(album)] = hide }
    }

    suspend fun setCustomCover(album: AlbumType, uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(coverKey(album))
            else prefs[coverKey(album)] = uri
        }
    }

    // ===== Per-album (folder path) =====
    fun albumOrderMode(path: String): Flow<SortMode> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[albumOrderKey(path)]
            raw?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
                ?: SortMode.RECENT_FIRST
        }

    fun albumHideDate(path: String): Flow<Boolean> = context.dataStore.data
        .map { it[albumHideDateKey(path)] ?: false }

    fun albumCustomCover(path: String): Flow<String?> = context.dataStore.data
        .map { it[albumCoverKey(path)] }

    suspend fun setAlbumOrderMode(path: String, mode: SortMode) {
        context.dataStore.edit { it[albumOrderKey(path)] = mode.name }
    }

    suspend fun setAlbumHideDate(path: String, hide: Boolean) {
        context.dataStore.edit { it[albumHideDateKey(path)] = hide }
    }

    suspend fun setAlbumCustomCover(path: String, uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(albumCoverKey(path))
            else prefs[albumCoverKey(path)] = uri
        }
    }

    // ===== Reset =====
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}