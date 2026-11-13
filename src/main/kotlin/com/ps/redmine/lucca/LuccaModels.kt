package com.ps.redmine.lucca

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class LuccaCookie(val name: String, val value: String)

@Serializable
data class LuccaSession(val userId: Int, val cookies: List<LuccaCookie>)

data class LuccaLeave(
    val date: LocalDate,
    val isAM: Boolean,
    val accountName: String,
    val isConfirmed: Boolean,
)

data class LuccaPerson(val id: Int, val firstName: String, val lastName: String)

enum class AbsencePeriod { FULL_DAY, MORNING, AFTERNOON }

data class AbsenceSlot(val accountName: String, val period: AbsencePeriod, val isConfirmed: Boolean)

data class AbsenceDay(val date: LocalDate, val slots: List<AbsenceSlot>)

/** Lucca stores one leave per half-day; a morning and an afternoon of the same account collapse into a full day. */
fun groupAbsencesByDay(leaves: List<LuccaLeave>): List<AbsenceDay> =
    leaves.groupBy { it.date }
        .toSortedMap()
        .map { (date, dayLeaves) ->
            val slots = dayLeaves.groupBy { it.accountName }.map { (account, halves) ->
                val hasMorning = halves.any { it.isAM }
                val hasAfternoon = halves.any { !it.isAM }
                val period = when {
                    hasMorning && hasAfternoon -> AbsencePeriod.FULL_DAY
                    hasMorning -> AbsencePeriod.MORNING
                    else -> AbsencePeriod.AFTERNOON
                }
                AbsenceSlot(account, period, isConfirmed = halves.all { it.isConfirmed })
            }.sortedBy { it.period }
            AbsenceDay(date, slots)
        }

@Serializable
internal data class LuccaDataResponse<T>(val data: T)

@Serializable
internal data class LuccaItems<T>(val items: List<T>)

@Serializable
internal data class LuccaUserDto(val id: Int)

@Serializable
internal data class LuccaPlanningUsersDto(val items: List<LuccaPlanningUserDto>, val count: Int? = null)

@Serializable
internal data class LuccaPlanningUserDto(val id: Int, val firstName: String = "", val lastName: String = "")

@Serializable
internal data class LuccaNamedDto(val id: Int, val name: String)

@Serializable
internal data class LuccaLeavePeriodDto(val id: Int, val isConfirmed: Boolean = false)

@Serializable
internal data class LuccaLeaveDto(
    val id: String,
    val date: String,
    val isAM: Boolean,
    val isActive: Boolean = true,
    val isRealLeave: Boolean = true,
    val leaveAccount: LuccaNamedDto? = null,
    val leavePeriod: LuccaLeavePeriodDto? = null,
)
