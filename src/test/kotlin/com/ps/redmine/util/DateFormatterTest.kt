package com.ps.redmine.util

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
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
        val formatted = DateFormatter.formatShort(testDate, Locale.FRENCH)
        assertEquals("25/12/2023", formatted)
    }

    @Test
    fun `test full date format in French locale`() {
        Locale.setDefault(Locale.FRENCH)
        val formatted = DateFormatter.formatFull(testDate, Locale.FRENCH)
        assertEquals("lundi 25 décembre 2023", formatted)
    }

    @Test
    fun `test short date format in English locale`() {
        Locale.setDefault(Locale.ENGLISH)
        val formatted = DateFormatter.formatShort(testDate, Locale.ENGLISH)
        assertEquals("12/25/23", formatted)
    }

    @Test
    fun `test full date format in English locale`() {
        Locale.setDefault(Locale.ENGLISH)
        val formatted = DateFormatter.formatFull(testDate, Locale.ENGLISH)
        assertEquals("Monday, December 25, 2023", formatted)
    }
}
