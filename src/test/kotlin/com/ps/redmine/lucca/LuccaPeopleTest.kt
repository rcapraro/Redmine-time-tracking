package com.ps.redmine.lucca

import com.ps.redmine.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LuccaPeopleTest {

    private val helene = LuccaPerson(1, "Hélène", "DUPONT-MARTIN")
    private val jean = LuccaPerson(2, "Jean", "DUPONT")
    private val julie = LuccaPerson(3, "Julie", "DUPONT")

    @Test
    fun `the same name matches whatever the case, accents and hyphens`() {
        val user = User(10, "Helene", "Dupont Martin", "hdupont-martin")

        assertEquals(helene, findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `a name written differently is found through the login rule`() {
        val user = User(10, "Juju", "Dupont", "jdupont")

        assertEquals(jean, findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `homonyms are never guessed between`() {
        val homonym = LuccaPerson(4, "Jean", "Dupont")

        assertNull(findLuccaPerson(User(10, "Jean", "Dupont", "jdupont"), listOf(jean, homonym)))
    }

    @Test
    fun `a login written as an e-mail address works too`() {
        val user = User(10, "Johnny", "Dupont", "jdupont@pack-solutions.com")

        assertEquals(jean, findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `nobody is picked when the login rule is ambiguous`() {
        val user = User(10, "J", "Dupont", "jdupont")

        assertNull(findLuccaPerson(user, listOf(jean, julie)))
    }

    @Test
    fun `someone outside the planning is not found`() {
        val user = User(10, "Paul", "Durand", "pdurand")

        assertNull(findLuccaPerson(user, listOf(jean, helene)))
    }
}
