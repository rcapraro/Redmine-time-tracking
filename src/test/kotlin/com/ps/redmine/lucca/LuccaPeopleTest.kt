package com.ps.redmine.lucca

import com.ps.redmine.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LuccaPeopleTest {

    private val helene = LuccaPerson(1, "Hélène", "DUPONT-MARTIN")
    private val jean = LuccaPerson(2, "Jean", "DUPONT")

    @Test
    fun `the same name matches whatever the case, accents and hyphens`() {
        val user = User(10, "Helene", "Dupont Martin", "hdupont-martin")

        assertEquals(helene, findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `a name written another way is the same name`() {
        val spellings = listOf(
            "Hélène" to "HELENE",
            "Hélène" to "He\u0301le\u0300ne",
            "François" to "Francois",
            "Noël" to "Noel",
            "Jean-Pierre" to "Jean Pierre",
            "Jean-Pierre" to "JeanPierre",
            "O'Brien" to "O\u2019Brien",
            "N'Diaye" to "Ndiaye",
            "N'Diaye" to "N\u02BCDiaye",
            "D'Angelo" to "D Angelo",
            "St. Clair" to "St Clair",
            "Lebœuf" to "LEBOEUF",
            "Lætitia" to "Laetitia",
            "Strauß" to "Strauss",
            "Søren" to "Soren",
            "Łukasz" to "Lukasz",
            "Đorđe" to "Dorde",
            "Đorđe" to "ÐORÐE",
            "Işık" to "Isik",
            "Ħamrun" to "Hamrun",
            "Ŧomas" to "Tomas",
            "Pal·larès" to "Pallares",
            "Nguyễn" to "Nguyen",
        )

        spellings.forEach { (redmine, lucca) ->
            val person = LuccaPerson(1, lucca, lucca)

            assertEquals(person, findLuccaPerson(User(10, redmine, redmine, "login"), listOf(person)), "$redmine / $lucca")
        }
    }

    @Test
    fun `another letter makes another name`() {
        val names = listOf(
            "Jean" to "Jeanne", "Dupont" to "Dupond", "Marie" to "Maria", "Lea" to "Leo", "Martin" to "Martine",
            "佐々木" to "佐木", "राम" to "रम",
        )

        names.forEach { (redmine, lucca) ->
            val person = LuccaPerson(1, lucca, "Durand")

            assertNull(findLuccaPerson(User(10, redmine, "Durand", "login"), listOf(person)), "$redmine / $lucca")
        }
    }

    @Test
    fun `the login never stands in for a name written with other letters`() {
        val user = User(10, "Juju", "Dupont", "jdupont")

        assertNull(findLuccaPerson(user, listOf(jean, helene)))
    }

    @Test
    fun `homonyms are never guessed between`() {
        val homonym = LuccaPerson(4, "Jean", "Dupont")

        assertNull(findLuccaPerson(User(10, "Jean", "Dupont", "jdupont"), listOf(jean, homonym)))
    }

    @Test
    fun `people told apart by an accent only are homonyms`() {
        val accented = LuccaPerson(4, "Jéan", "Dupont")

        assertNull(findLuccaPerson(User(10, "Jean", "Dupont", "jdupont"), listOf(jean, accented)))
    }

    @Test
    fun `letters beyond the basic plane are compared, not dropped`() {
        val person = LuccaPerson(1, "Taro", "\uD842\uDFB7田")

        assertNull(findLuccaPerson(User(10, "Taro", "田", "login"), listOf(person)))
    }

    @Test
    fun `a user without a last name matches nobody`() {
        val nameless = LuccaPerson(5, "Jean", "")

        assertNull(findLuccaPerson(User(10, "Jean", "-", "jean"), listOf(nameless)))
    }
}
