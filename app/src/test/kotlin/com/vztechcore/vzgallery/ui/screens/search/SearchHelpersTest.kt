package com.vztechcore.vzgallery.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHelpersTest {

    // ==================== normalizeText ====================

    @Test
    fun `normalize removes acute accent and lowercases`() {
        assertEquals("video", normalizeText("Vídeo"))
    }

    @Test
    fun `normalize handles fully uppercase`() {
        assertEquals("video", normalizeText("VÍDEO"))
    }

    @Test
    fun `normalize handles tilde`() {
        assertEquals("nao", normalizeText("Não"))
    }

    @Test
    fun `normalize handles cedilla`() {
        assertEquals("coracao", normalizeText("Coração"))
    }

    @Test
    fun `normalize handles multiple accents in one word`() {
        assertEquals("acao", normalizeText("Ação"))
    }

    @Test
    fun `normalize handles empty string`() {
        assertEquals("", normalizeText(""))
    }

    @Test
    fun `normalize handles plain ascii text`() {
        assertEquals("hello world", normalizeText("Hello World"))
    }

    // ==================== containsText ====================

    @Test
    fun `contains finds plain substring`() {
        assertTrue(containsText("minha foto", "foto"))
    }

    @Test
    fun `contains ignores case`() {
        assertTrue(containsText("MINHA FOTO", "foto"))
    }

    @Test
    fun `contains ignores accents in target`() {
        assertTrue(containsText("Vídeo legal", "video"))
    }

    @Test
    fun `contains ignores accents in query`() {
        assertTrue(containsText("video legal", "vídeo"))
    }

    @Test
    fun `contains returns false when no match`() {
        assertFalse(containsText("foto", "video"))
    }

    @Test
    fun `contains returns false for empty query`() {
        assertFalse(containsText("foto", ""))
    }

    @Test
    fun `contains returns false for blank query with spaces`() {
        assertFalse(containsText("foto", "   "))
    }

    @Test
    fun `contains returns true for exact match`() {
        assertTrue(containsText("foto", "foto"))
    }

    @Test
    fun `contains returns false when query is longer than target`() {
        assertFalse(containsText("fo", "foto"))
    }

    // ==================== indexOfText ====================

    @Test
    fun `indexOf finds position in middle`() {
        assertEquals(6, indexOfText("minha foto", "foto"))
    }

    @Test
    fun `indexOf finds position at start`() {
        assertEquals(0, indexOfText("foto legal", "foto"))
    }

    @Test
    fun `indexOf ignores accents`() {
        assertEquals(0, indexOfText("Vídeo", "video"))
    }

    @Test
    fun `indexOf ignores case`() {
        assertEquals(0, indexOfText("FOTO", "foto"))
    }

    @Test
    fun `indexOf returns -1 when not found`() {
        assertEquals(-1, indexOfText("foto", "video"))
    }

    @Test
    fun `indexOf returns -1 for empty query`() {
        assertEquals(-1, indexOfText("foto", ""))
    }

    @Test
    fun `indexOf returns -1 for blank query`() {
        assertEquals(-1, indexOfText("foto", "   "))
    }
}