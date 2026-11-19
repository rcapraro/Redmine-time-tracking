package com.ps.redmine.lucca

import com.ps.redmine.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** [inPlanning]: the signed-in user has that person, and so their absences, in their own Lucca planning. */
data class LuccaMatch(val person: LuccaPerson, val inPlanning: Boolean)

/** Nobody matched, but Lucca refused a list of people the person may well be in. */
class LuccaPeopleUnreadableException(cause: LuccaApiException) : Exception(cause.message, cause)

/**
 * The Lucca person a Redmine user stands for: same first and last name, whatever the case. Homonyms give no match
 * rather than a guess.
 */
fun findLuccaPerson(user: User, people: List<LuccaPerson>): LuccaPerson? =
    if (user.lastName.isBlank()) null else people.singleOrNull {
        it.firstName.equals(user.firstName, ignoreCase = true) && it.lastName.equals(user.lastName, ignoreCase = true)
    }

/**
 * Looks among the whole directory and the signed-in user's planning together, so that homonyms are told apart across
 * the company. Lucca may refuse either list: the other one is enough to find someone, not to say nobody matches.
 */
suspend fun LuccaClient.findPerson(cookies: List<LuccaCookie>, user: User): LuccaMatch? = coroutineScope {
    val directoryRead = async { readPeople { directoryPeople(cookies) } }
    val planningRead = async { readPeople { planningPeople(cookies) } }
    val directory = directoryRead.await()
    val planning = planningRead.await()
    val person = findLuccaPerson(user, (directory.people + planning.people).distinctBy { it.id })
    if (person == null) (directory.refusal ?: planning.refusal)?.let { throw LuccaPeopleUnreadableException(it) }
    person?.let { LuccaMatch(it, inPlanning = planning.people.any { colleague -> colleague.id == it.id }) }
}

private class PeopleRead(val people: List<LuccaPerson>, val refusal: LuccaApiException? = null)

private inline fun readPeople(read: () -> List<LuccaPerson>): PeopleRead =
    try {
        PeopleRead(read())
    } catch (e: LuccaApiException) {
        PeopleRead(emptyList(), e)
    }
