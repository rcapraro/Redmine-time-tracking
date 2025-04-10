package com.ps.redmine.model

import kotlinx.datetime.LocalDate

data class TimeEntry(
    val id: Int? = null,
    val date: LocalDate,
    val hours: Float,
    val activity: Activity,
    val project: Project,
    val issue: Issue,
    val comments: String? = null
)

data class Activity(
    val id: Int,
    val name: String
)

data class Project(
    val id: Int,
    val name: String
)

data class Issue(
    val id: Int,
    val subject: String
)
