package com.vztechcore.vzgallery.data

import com.vztechcore.vzgallery.model.GroupData
import com.vztechcore.vzgallery.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DataGrouper {

    private const val ONE_DAY = 24L * 60 * 60 * 1000

    fun group(items: List<MediaItem>): List<GroupData> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfYesterday = startOfToday - ONE_DAY
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Created once per call (SimpleDateFormat is expensive).
        // All locals → no thread-safety issue.
        val sdfNoYear   = SimpleDateFormat("d 'de' MMM", Locale("pt", "BR"))
        val sdfWithYear = SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("pt", "BR"))

        fun formatExactDate(ms: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            val sdf = if (cal.get(Calendar.YEAR) == currentYear) sdfNoYear else sdfWithYear
            return sdf.format(Date(ms)).replaceFirstChar { it.uppercase() }
        }

        // Cache one formatted label per day-bucket.
        val dateLabelCache = HashMap<Long, String>()

        val groups = linkedMapOf<String, MutableList<MediaItem>>()
        items.forEach { item ->
            val dateMs = item.dataAdicionada * 1000
            val key = when {
                dateMs >= startOfToday -> "Hoje"
                dateMs >= startOfYesterday -> "Ontem"
                else -> {
                    val dayStart = startOfDay(dateMs)
                    dateLabelCache.getOrPut(dayStart) { formatExactDate(dateMs) }
                }
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.map { (title, list) -> GroupData(title, list) }
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
