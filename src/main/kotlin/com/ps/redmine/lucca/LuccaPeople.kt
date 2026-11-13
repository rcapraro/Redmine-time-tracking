package com.ps.redmine.lucca

import com.ps.redmine.model.User
import java.text.Normalizer

/**
 * The Lucca person a Redmine user stands for: same first and last name, ignoring case, accents, spaces and hyphens.
 * A name written differently in each tool falls back on the rule Redmine logins follow at Pack Solutions, as e-mail
 * addresses do: first letter of the first name, then the last name ("jdupont"). Homonyms, or a login matching several
 * people, give no match rather than a guess.
 */
fun findLuccaPerson(user: User, people: List<LuccaPerson>): LuccaPerson? {
    val byName = if (nameKey(user.lastName).isEmpty()) emptyList() else people.filter {
        nameKey(it.firstName) == nameKey(user.firstName) && nameKey(it.lastName) == nameKey(user.lastName)
    }
    if (byName.size == 1) return byName.single()
    val login = user.login.substringBefore('@').lowercase()
    return byName.ifEmpty { people }.singleOrNull { loginKey(it) == login }
}

private val combiningMarks = Regex("\\p{M}")

private fun nameKey(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase().filter { it.isLetter() }

private fun loginKey(person: LuccaPerson) = nameKey(person.firstName).take(1) + nameKey(person.lastName)
