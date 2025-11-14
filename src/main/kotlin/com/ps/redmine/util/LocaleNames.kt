package com.ps.redmine.util

import java.util.*

object LocaleNames {
    private val monthsEnFull = arrayOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )
    private val monthsFrFull = arrayOf(
        "janvier",
        "février",
        "mars",
        "avril",
        "mai",
        "juin",
        "juillet",
        "août",
        "septembre",
        "octobre",
        "novembre",
        "décembre"
    )

    private val weekdaysEnShort = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val weekdaysFrShort = arrayOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    private val weekdaysEnFull = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val weekdaysFrFull = arrayOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")

    fun monthName(month: Int, locale: Locale, full: Boolean = true): String {
        val idx = month - 1
        return when (locale.language.lowercase()) {
            "en" -> if (full) monthsEnFull[idx] else monthsEnFull[idx].take(3)
            else -> if (full) monthsFrFull[idx] else monthsFrFull[idx].take(3)
        }
    }

    fun weekdayName(isoDay: Int, locale: Locale, full: Boolean = true): String {
        val idx = isoDay - 1
        return when (locale.language.lowercase()) {
            "en" -> if (full) weekdaysEnFull[idx] else weekdaysEnShort[idx]
            else -> if (full) weekdaysFrFull[idx] else weekdaysFrShort[idx]
        }
    }
}
