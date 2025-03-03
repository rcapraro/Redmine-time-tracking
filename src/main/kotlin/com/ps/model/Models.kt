package com.ps.model

import kotlinx.datetime.LocalDate

data class TimeEntry(
    val id: Int? = null,
    val date: LocalDate,
    val hours: Float,
    val activity: Activity,
    val project: Project,
    val comments: String
)

data class Activity(
    val id: Int,
    val name: String
)

data class Project(
    val id: Int,
    val name: String
)