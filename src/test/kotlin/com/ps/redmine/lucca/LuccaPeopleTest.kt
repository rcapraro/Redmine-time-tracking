package com.ps.redmine.lucca

import com.ps.redmine.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LuccaPeopleTest {

    private val helene = LuccaPerson(1, "Hélène", "DUPONT-MARTIN")
    private val jean = LuccaPerson(2, "Jean", "DUPONT")

    @Test
    fun `the same name matches whatever the case`() {
        val user = User(10, "hélène", "Dupont-Martin", "hdupont-martin")

        assertEquals(helene, findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `a name without its accent is another name`() {
        val user = User(10, "Helene", "Dupont-Martin", "hdupont-martin")

        assertNull(findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `a name without its hyphen is another name`() {
        val user = User(10, "Hélène", "Dupont Martin", "hdupont-martin")

        assertNull(findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `the login never stands in for a name written differently`() {
        val user = User(10, "Juju", "Dupont", "jdupont")

        assertNull(findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `homonyms are never guessed between`() {
        val homonym = LuccaPerson(4, "Jean", "Dupont")

        assertNull(findLuccaPerson(User(10, "Jean", "Dupont", "jdupont"), listOf(jean, homonym)))
    }

    @Test
    fun `a user without a last name matches nobody`() {
        val nameless = LuccaPerson(5, "Jean", "")

        assertNull(findLuccaPerson(User(10, "Jean", "", "jean"), listOf(nameless)))
    }
}
