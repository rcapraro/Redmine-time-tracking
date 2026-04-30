package com.ps.redmine.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ps.redmine_time.generated.resources.Res
import com.ps.redmine_time.generated.resources.inter_bold
import com.ps.redmine_time.generated.resources.inter_medium
import com.ps.redmine_time.generated.resources.inter_regular
import com.ps.redmine_time.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun rememberInterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

/**
 * Compact, dense desktop type scale built on Inter.
 *
 * Tracks the M3 baseline structure but tunes sizes/line-heights/tracking
 * for a desktop window (smaller targets, denser layouts) and improves
 * readability on body text.
 */
@Composable
fun appTypography(): Typography {
    val inter = rememberInterFontFamily()
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.with(inter),
        displayMedium = base.displayMedium.with(inter),
        displaySmall = base.displaySmall.with(inter),
        headlineLarge = base.headlineLarge.with(inter),
        headlineMedium = base.headlineMedium.with(inter),
        headlineSmall = base.headlineSmall.with(inter),
        titleLarge = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp,
        ),
        titleMedium = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Medium,
            fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = inter, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
        ),
    )
}

private fun TextStyle.with(family: FontFamily): TextStyle = copy(fontFamily = family)
