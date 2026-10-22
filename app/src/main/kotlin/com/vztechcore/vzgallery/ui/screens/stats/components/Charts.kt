package com.vztechcore.vzgallery.ui.screens.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.ui.theme.VzOnBackground
import com.vztechcore.vzgallery.ui.theme.VzOnSurfaceVariant
import com.vztechcore.vzgallery.ui.theme.VzSecondary

data class ChartSlice(
    val label: String,
    val value: Int,
    val color: Color
)

// ============================================================
// HORIZONTAL BARS — "two slide" style (fill + track)
// ============================================================
@Composable
fun HorizontalBarChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    progress: Float = 1f
) {
    val maxValue = slices.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val trackColor = VzOnSurfaceVariant.copy(alpha = 0.18f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        slices.forEach { slice ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = slice.label.uppercase(),
                        color = VzOnBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = (slice.value * progress).toInt().toString(),
                        color = VzOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))

                // Dual track: colored fill + gray track SEPARATED by a gap
                val fillFraction = ((slice.value.toFloat() / maxValue) * progress)
                    .coerceIn(0.001f, 0.999f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(fillFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(slice.color)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f - fillFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(trackColor)
                    )
                }
            }
        }
    }
}

// ============================================================
// MONTHLY BAR CHART (vertical, with value on top)
// ============================================================
@Composable
fun MonthlyBarChart(
    values: List<Pair<String, Int>>, // ("Jan", 120)
    modifier: Modifier = Modifier,
    progress: Float = 1f
) {
    val maxValue = values.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val maxBarHeight = 88.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEach { (_, v) ->
                val fraction = (v.toFloat() / maxValue) * progress
                val barHeight = (maxBarHeight.value * fraction).dp

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = (v * progress).toInt().toString(),
                        color = VzSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(barHeight.coerceAtLeast(1.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(VzSecondary)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            values.forEach { (label, _) ->
                Text(
                    text = label,
                    color = VzOnSurfaceVariant,
                    fontSize = 9.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// DONUT CHART (with animated sweep)
// ============================================================
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 22.dp,
    progress: Float = 1f
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val radius = (this.size.minDimension - sw) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)

            val targetAngle = 360f * progress.coerceIn(0f, 1f)
            var cumulative = 0f

            slices.forEach { slice ->
                val sliceStart = cumulative
                val sliceEnd = cumulative + (slice.value.toFloat() / total) * 360f

                val visibleStart = sliceStart.coerceAtMost(targetAngle)
                val visibleEnd = sliceEnd.coerceAtMost(targetAngle)

                if (visibleEnd > visibleStart) {
                    drawArc(
                        color = slice.color,
                        startAngle = -90f + visibleStart,
                        sweepAngle = visibleEnd - visibleStart,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = sw, cap = StrokeCap.Butt)
                    )
                }
                cumulative = sliceEnd
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = (total * progress).toInt().toString(),
                color = VzOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "itens",
                color = VzOnSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// ============================================================
// LEGEND FOR THE DONUT
// ============================================================
@Composable
fun ChartLegend(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    progress: Float = 1f
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        slices.forEach { slice ->
            val pct = (slice.value * 100f / total).toInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(slice.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = slice.label,
                    color = VzOnBackground,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(pct * progress).toInt()}%",
                    color = VzOnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}