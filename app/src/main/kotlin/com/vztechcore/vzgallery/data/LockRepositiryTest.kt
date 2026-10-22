package com.vztechcore.vzgallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LockRepositoryTest {

    @Test
    fun `salt has 32 hex chars (16 bytes)`() {
        val salt = LockRepository.generateSalt()
        assertEquals(32, salt.length)
        assertTrue(salt.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `salt is unique each call`() {
        val s1 = LockRepository.generateSalt()
        val s2 = LockRepository.generateSalt()
        assertNotEquals(s1, s2)
    }

    @Test
    fun `hash is deterministic for same input`() {
        val salt = "0123456789abcdef0123456789abcdef"
        val h1 = LockRepository.hashPassword("123456", salt)
        val h2 = LockRepository.hashPassword("123456", salt)
        assertEquals(h1, h2)
        assertEquals(64, h1.length) // SHA-256 = 64 hex chars
    }

    @Test
    fun `hash differs with different salts`() {
        val h1 = LockRepository.hashPassword("123456", "saltaaa")
        val h2 = LockRepository.hashPassword("123456", "saltbbb")
        assertNotEquals(h1, h2)
    }

    @Test
    fun `hash differs with different passwords`() {
        val salt = "somesalt"
        val h1 = LockRepository.hashPassword("123456", salt)
        val h2 = LockRepository.hashPassword("123457", salt)
        assertNotEquals(h1, h2)
    }

    @Test
    fun `verify accepts correct password`() {
        val salt = LockRepository.generateSalt()
        val hash = LockRepository.hashPassword("mypassword", salt)
        assertTrue(LockRepository.verifyPassword("mypassword", salt, hash))
    }

    @Test
    fun `verify rejects wrong password`() {
        val salt = LockRepository.generateSalt()
        val hash = LockRepository.hashPassword("mypassword", salt)
        assertFalse(LockRepository.verifyPassword("wrongpassword", salt, hash))
    }

    @Test
    fun `verify rejects wrong salt`() {
        val salt = LockRepository.generateSalt()
        val hash = LockRepository.hashPassword("mypassword", salt)
        val wrongSalt = LockRepository.generateSalt()
        assertFalse(LockRepository.verifyPassword("mypassword", wrongSalt, hash))
    }

    @Test
    fun `verify rejects hash of different length`() {
        assertFalse(LockRepository.verifyPassword("123", "salt", "shorthash"))
    }

    @Test
    fun `verify handles empty password`() {
        val salt = LockRepository.generateSalt()
        val hash = LockRepository.hashPassword("", salt)
        assertTrue(LockRepository.verifyPassword("", salt, hash))
        assertFalse(LockRepository.verifyPassword("x", salt, hash))
    }

    @Test
    fun `verify handles unicode password`() {
        val salt = LockRepository.generateSalt()
        val hash = LockRepository.hashPassword("senh@ção123", salt)
        assertTrue(LockRepository.verifyPassword("senh@ção123", salt, hash))
        assertFalse(LockRepository.verifyPassword("senhacao123", salt, hash))
    }

    @Test
    fun `different PINs produce different hashes`() {
        val salt = LockRepository.generateSalt()
        val pin1 = LockRepository.hashPassword("123456", salt)
        val pin2 = LockRepository.hashPassword("654321", salt)
        assertNotEquals(pin1, pin2)
    }
}