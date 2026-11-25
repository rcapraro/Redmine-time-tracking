package com.ps.redmine.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * A duration in whole seconds, so that sums and comparisons are exact: hours typed with two decimals (0.01 h is 36 s)
 * and minutes logged in Redmine (1h20 is stored as 1.3333 h, 4800 s) both add up without rounding drift.
 */
@JvmInline
value class Hours private constructor(private val seconds: Int) : Comparable<Hours> {

    operator fun plus(other: Hours) = Hours(seconds + other.seconds)

    operator fun minus(other: Hours) = Hours(seconds - other.seconds)

    operator fun times(factor: Int) = Hours(seconds * factor)

    operator fun div(other: Hours): Double = seconds.toDouble() / other.seconds

    override fun compareTo(other: Hours) = seconds.compareTo(other.seconds)

    fun toFloat(): Float = seconds / SECONDS_PER_HOUR.toFloat()

    /** Rounded to the two decimals Redmine shows. */
    fun toBigDecimal(): BigDecimal =
        BigDecimal.valueOf(seconds.toLong()).divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP)

    /** Exactly 100 when these hours equal [target]: both are whole numbers of seconds. */
    fun percentOf(target: Hours): Float = (seconds * 100.0 / target.seconds).toFloat()

    /** One or two decimals: "7.5", "3.75", "8.0". */
    fun format(locale: Locale = Locale.getDefault(Locale.Category.FORMAT)): String =
        DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(locale)).format(toBigDecimal())

    override fun toString() = format(Locale.ROOT)

    companion object {
        private const val SECONDS_PER_HOUR = 3600L

        val ZERO = Hours(0)

        /** Hours as typed so far: digits, then an optional `.` or `,` and up to two decimals. */
        val INPUT_REGEX = Regex("^\\d*([\\.,]\\d{0,2})?$")

        fun of(hours: Double): Hours = ofHours(BigDecimal.valueOf(hours)) ?: throw ArithmeticException("$hours h")

        /** Reads hours typed with a `.` or a `,`; null when the text is not a number or too large to be hours. */
        fun parse(text: String): Hours? = text.replace(',', '.').toBigDecimalOrNull()?.let(::ofHours)

        fun <T> sumOf(items: Iterable<T>, selector: (T) -> Hours): Hours = Hours(items.sumOf { selector(it).seconds })

        private fun ofHours(hours: BigDecimal): Hours? =
            runCatching {
                Hours(hours.multiply(BigDecimal.valueOf(SECONDS_PER_HOUR)).setScale(0, RoundingMode.HALF_UP).intValueExact())
            }.getOrNull()
    }
}

fun <T> Iterable<T>.sumOfHours(selector: (T) -> Hours): Hours = Hours.sumOf(this, selector)
