package com.vztechcore.vzgallery.data

import android.net.Uri
import com.vztechcore.vzgallery.model.MediaItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DataGrouperTest {

    private fun fakeItem(id: Long, dateAddedSec: Long, isVideo: Boolean = false): MediaItem {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://media/$id"
        return MediaItem(
            id = id,
            uri = uri,
            nome = "item_$id.${if (isVideo) "mp4" else "jpg"}",
            dataAdicionada = dateAddedSec,
            isVideo = isVideo,
            duracaoMs = if (isVideo) 5000L else null,
            relativePath = "/DCIM/",
            tamanhoBytes = 1000L
        )
    }

    private fun daysAgo(n: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -n)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 1000L
    }

    @Test
    fun `empty list returns empty groups`() {
        val groups = DataGrouper.group(emptyList())
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `single today item groups under Hoje`() {
        val item = fakeItem(1, daysAgo(0))
        val groups = DataGrouper.group(listOf(item))
        assertEquals(1, groups.size)
        assertEquals("Hoje", groups[0].titulo)
        assertEquals(1, groups[0].itens.size)
    }

    @Test
    fun `yesterday item groups under Ontem`() {
        val item = fakeItem(1, daysAgo(1))
        val groups = DataGrouper.group(listOf(item))
        assertEquals(1, groups.size)
        assertEquals("Ontem", groups[0].titulo)
        assertEquals(1, groups[0].itens.size)
    }

    @Test
    fun `items from same day in same group`() {
        val today1 = fakeItem(1, daysAgo(0))
        val today2 = fakeItem(2, daysAgo(0))
        val groups = DataGrouper.group(listOf(today1, today2))
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].itens.size)
    }

    @Test
    fun `different days produce different groups`() {
        val today = fakeItem(1, daysAgo(0))
        val yesterday = fakeItem(2, daysAgo(1))
        val groups = DataGrouper.group(listOf(today, yesterday))
        assertEquals(2, groups.size)
    }

    @Test
    fun `video and image on same day in same group`() {
        val img = fakeItem(1, daysAgo(0), isVideo = false)
        val vid = fakeItem(2, daysAgo(0), isVideo = true)
        val groups = DataGrouper.group(listOf(img, vid))
        assertEquals(1, groups.size)
        assertEquals(2, groups[0].itens.size)
    }

    @Test
    fun `old item does not fall in Hoje or Ontem`() {
        val old = fakeItem(1, daysAgo(30))
        val groups = DataGrouper.group(listOf(old))
        assertEquals(1, groups.size)
        assertTrue(groups[0].titulo != "Hoje")
        assertTrue(groups[0].titulo != "Ontem")
    }
}