package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import java.util.Locale
import kotlin.test.assertEquals

class DateFormatterTest {
    private lateinit var originalLocale: Locale
    private val testDate = LocalDate(2023, 12, 25)

    @BeforeEach
    fun setup() {
        originalLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `test short date format in French locale`() {
        Locale.setDefault(Locale.FRENCH)
        val formatted = DateFormatter.formatShort(testDate)
        assertEquals("25/12/2023", formatted)
    }

    @Test
    fun `test full date format in French locale`() {
        Locale.setDefault(Locale.FRENCH)
        val formatted = DateFormatter.formatFull(testDate)
        assertEquals("lundi 25 décembre 2023", formatted)
    }

    @Test
    fun `test short date format in English locale`() {
        Locale.setDefault(Locale.ENGLISH)
        val formatted = DateFormatter.formatShort(testDate)
        assertEquals("12/25/23", formatted)
    }

    @Test
    fun `test full date format in English locale`() {
        Locale.setDefault(Locale.ENGLISH)
        val formatted = DateFormatter.formatFull(testDate)
        assertEquals("Monday, December 25, 2023", formatted)
    }
}