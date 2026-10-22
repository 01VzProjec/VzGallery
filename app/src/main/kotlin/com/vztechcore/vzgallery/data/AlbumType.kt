package com.vztechcore.vzgallery.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

enum class AlbumType(
    val title: String,
    val icon: ImageVector
) {
    ALL_PHOTOS("Todas as fotos", Icons.Outlined.PhotoLibrary),
    FAVORITES("Favoritos", Icons.Outlined.FavoriteBorder),
    CAMERA("Câmera", Icons.Outlined.PhotoCamera),
    ALL_VIDEOS("Todos os vídeos", Icons.Outlined.Videocam)
}

enum class SortMode(val label: String) {
    RECENT_FIRST("Mais recentes"),
    OLDEST_FIRST("Mais antigos"),
    SIZE_ASC("Tamanho (menor → maior)"),
    SIZE_DESC("Tamanho (maior → menor)"),
    NAME_ASC("Nome (A → Z)")
}

enum class TimeFilter(val label: String) {
    LAST_WEEK("Última semana"),
    LAST_30_DAYS("Último mês"),
    LAST_90_DAYS("Último trimestre"),
    THIS_YEAR("Este ano");

    fun startTimeMs(): Long {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        return when (this) {
            LAST_WEEK -> now - 7 * dayMs
            LAST_30_DAYS -> now - 30 * dayMs
            LAST_90_DAYS -> now - 90 * dayMs
            THIS_YEAR -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
    }
}