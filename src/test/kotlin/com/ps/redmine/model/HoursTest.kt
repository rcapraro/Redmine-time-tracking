package com.ps.redmine.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class HoursTest {

    @Test
    fun `sums of decimal hours are exact`() {
        val day = listOf(3.75, 3.7, 0.05).map(Hours::of).sumOfHours { it }

        assertEquals(Hours.of(7.5), day)
        assertEquals(Hours.of(7.5), Hours.of(0.1) * 75)
    }

    @Test
    fun `a month of days adds up to exactly the expected hours`() {
        val month = List(23) { Hours.of(3.7) + Hours.of(3.8) }.sumOfHours { it }

        assertEquals(Hours.of(7.5) * 23, month)
        assertEquals(100f, month.percentOf(Hours.of(7.5) * 23))
    }

    @Test
    fun `one hundredth short is never complete`() {
        val target = Hours.of(7.5) * 23
        val logged = target - Hours.of(0.01)

        assertTrue(logged < target)
        assertTrue(logged.percentOf(target) < 100f)
    }

    @Test
    fun `hours read from Redmine as floats are the hours logged`() {
        assertEquals(Hours.of(3.7), Hours.of(3.7f.toDouble()))
        assertEquals(Hours.of(3.75), Hours.of(3.75f.toDouble()))
    }

    @Test
    fun `minutes logged in Redmine add up exactly`() {
        val oneHourTwenty = Hours.of((4f / 3).toDouble())
        val day = List(3) { oneHourTwenty }.sumOfHours { it } + Hours.of(3.5)

        assertEquals(Hours.of(7.5), day)
        assertEquals("1.33", oneHourTwenty.format(Locale.US))
    }

    @Test
    fun `hours sent to Redmine keep their two decimals`() {
        assertEquals(3.75f, Hours.of(3.75).toFloat())
        assertEquals(3.7f, Hours.of(3.7).toFloat())
    }

    @Test
    fun `typed hours accept a dot or a comma`() {
        assertEquals(Hours.of(3.75), Hours.parse("3.75"))
        assertEquals(Hours.of(3.75), Hours.parse("3,75"))
        assertEquals(Hours.of(3.0), Hours.parse("3."))
        assertEquals(Hours.of(0.5), Hours.parse(",5"))
    }

    @Test
    fun `hours are typed with up to two decimals`() {
        listOf("", "3", "3.", "3,7", "3.75", ",5").forEach { assertTrue(it.matches(Hours.INPUT_REGEX), it) }
        listOf("3.751", "3,75,", "a", "-1").forEach { assertFalse(it.matches(Hours.INPUT_REGEX), it) }
    }

    @Test
    fun `text that is not hours parses to null`() {
        listOf("", ".", "abc", "99999999999").forEach { assertNull(Hours.parse(it), it) }
    }

    @Test
    fun `hours show one or two decimals`() {
        assertEquals("3.75", Hours.of(3.75).format(Locale.US))
        assertEquals("3,75", Hours.of(3.75).format(Locale.FRANCE))
        assertEquals("7.5", Hours.of(7.5).format(Locale.US))
        assertEquals("8.0", Hours.of(8.0).format(Locale.US))
        assertEquals("127.5", (Hours.of(7.5) * 17).format(Locale.US))
    }
}
