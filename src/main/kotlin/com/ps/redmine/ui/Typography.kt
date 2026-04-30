package com.ps.redmine.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ps.redmine_time.generated.resources.*
import org.jetbrains.compose.resources.Font

@Composable
fun rememberInterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

/**
 * Inter type scale — only **three sizes** are used in the general UI (18 / 14 / 12 sp).
 * A fourth, 10 sp, is reserved exclusively for the weekly progress-bar labels on the left
 * (their 16 dp wide bars cannot fit anything bigger). Nothing else in the app should land
 * at 10 sp, and there should be no raw `fontSize = X.sp` overrides anywhere else.
 *
 *   18 sp / SemiBold  → titleLarge, titleMedium, titleSmall  (titles, dialog headers)
 *   14 sp / Normal    → bodyLarge, bodyMedium                (body text, list items, dropdowns)
 *   12 sp             → labelLarge (Medium), labelMedium (Medium), bodySmall (Normal)
 *                       (badges, buttons, status pills, captions, helpers, errors)
 *   10 sp / Medium    → labelSmall                           (weekly-bar labels ONLY)
 */
@Composable
fun appTypography(): Typography {
    val inter = rememberInterFontFamily()
    val base = Typography()

    val title = TextStyle(
        fontFamily = inter, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp,
    )
    val body = TextStyle(
        fontFamily = inter, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp,
    )
    val label = TextStyle(
        fontFamily = inter, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp,
    )
    val caption = TextStyle(
        fontFamily = inter, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp,
    )
    // 10 sp — used **only** by WeeklyProgressBars labels (16 dp bars cannot fit larger
    // text). Routing access to it through `labelSmall` keeps the rule "no raw fontSize
    // overrides" intact while carving out one explicit exception.
    val weeklyBar = TextStyle(
        fontFamily = inter, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp,
    )

    return base.copy(
        displayLarge = base.displayLarge.with(inter),
        displayMedium = base.displayMedium.with(inter),
        displaySmall = base.displaySmall.with(inter),
        headlineLarge = base.headlineLarge.with(inter),
        headlineMedium = base.headlineMedium.with(inter),
        headlineSmall = base.headlineSmall.with(inter),
        titleLarge = title,
        titleMedium = title,
        titleSmall = title,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = label,
        labelMedium = label,
        labelSmall = weeklyBar,
    )
}

private fun TextStyle.with(family: FontFamily): TextStyle = copy(fontFamily = family)
