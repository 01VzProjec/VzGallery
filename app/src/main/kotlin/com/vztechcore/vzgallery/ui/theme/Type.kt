package com.vztechcore.vzgallery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vztechcore.vzgallery.R

// Fonte Plus Jakarta Sans (Google Fonts)
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),      // 400
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),       // 500
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),   // 600
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),           // 700
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)  // 800
)

val VzTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 42.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    )
)