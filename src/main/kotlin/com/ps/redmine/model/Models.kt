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

data class User(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val login: String,
    val admin: Boolean = false,
) {
    val displayName: String
        get() = listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { login }
}
