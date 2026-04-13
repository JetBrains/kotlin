package hogwarts

import org.junit.Test
import org.junit.Assert.*

class WizardTest {
    @Test
    fun testWizardCastsSpell() {
        val harry = Wizard("Harry Potter", "Gryffindor")
        val spell = Spell("Expelliarmus", 5)
        assertEquals("Harry Potter casts Expelliarmus!", harry.cast(spell))
    }

    @Test
    fun testFavoriteSpell() {
        val harry = Wizard("Harry Potter", "Gryffindor")
        val spell = harry.favoriteSpell()
        assertEquals("Expelliarmus", spell.incantation)
        assertEquals(5, spell.powerLevel)
    }
}
