package com.ps.util

import kotlinx.datetime.*
import java.time.LocalDate as JavaLocalDate
import java.time.YearMonth as JavaYearMonth

val today: LocalDate
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun LocalDate.toJava(): JavaLocalDate = JavaLocalDate.of(year, monthNumber, dayOfMonth)

fun JavaLocalDate.toKotlin(): LocalDate = LocalDate(year, monthValue, dayOfMonth)

fun LocalDate.toJavaYearMonth(): JavaYearMonth = JavaYearMonth.of(year, monthNumber)