package com.ps.redmine.lucca

import com.ps.redmine.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.Collator
import java.text.RuleBasedCollator
import java.util.Locale

/** [inPlanning]: the signed-in user has that person, and so their absences, in their own Lucca planning. */
data class LuccaMatch(val person: LuccaPerson, val inPlanning: Boolean)

/** Nobody matched, but Lucca refused a list of people the person may well be in. */
class LuccaPeopleUnreadableException(cause: LuccaApiException) : Exception(cause.message, cause)

/**
 * The Lucca person a Redmine user stands for: same letters in the first and last name, whatever the case, the accents
 * and what separates the letters (hyphens, apostrophes, spaces). Homonyms give no match rather than a guess.
 */
fun findLuccaPerson(user: User, people: List<LuccaPerson>): LuccaPerson? {
    val firstName = lettersOf(user.firstName)
    val lastName = lettersOf(user.lastName)
    if (lastName.isEmpty()) return null
    return people.singleOrNull { sameLetters(it.firstName, firstName) && sameLetters(it.lastName, lastName) }
}

// Unicode does not decompose letters such as ø, ł or ı as it does accented ones, so the JDK rules take them for
// letters of their own.
private const val UNDECOMPOSED_LETTER_RULES = "& o ; ø & O ; Ø & l ; ł & L ; Ł & d ; đ ; ð & D ; Đ ; Ð & i ; ı " +
    "& h ; ħ & H ; Ħ & t ; ŧ & T ; Ŧ"

// At primary strength a collator only tells base letters apart: "É" is "e", and "œ" is "oe".
private val letterCollator: Collator = Collator.getInstance(Locale.ROOT).let { root ->
    (root as? RuleBasedCollator)?.let { RuleBasedCollator(it.rules + UNDECOMPOSED_LETTER_RULES) } ?: root
}.apply {
    strength = Collator.PRIMARY
    decomposition = Collator.FULL_DECOMPOSITION
}

private fun sameLetters(name: String, letters: String) = letterCollator.compare(lettersOf(name), letters) == 0

// ʼ, ʻ and ʽ are modifier letters to Unicode, but apostrophes in names; other modifier letters, such as 々, are letters.
private val APOSTROPHE_LETTERS = setOf(0x02BC, 0x02BB, 0x02BD)

private val MARK_TYPES = setOf(Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK)
    .map { it.toInt() }

private fun lettersOf(name: String): String = name.codePoints()
    .filter { (Character.isLetter(it) && it !in APOSTROPHE_LETTERS) || Character.getType(it) in MARK_TYPES }
    .collect(::StringBuilder, StringBuilder::appendCodePoint, StringBuilder::append)
    .toString()

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
