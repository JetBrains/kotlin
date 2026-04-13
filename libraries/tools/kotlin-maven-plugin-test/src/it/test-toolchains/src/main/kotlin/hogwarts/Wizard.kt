package hogwarts

class Wizard(val name: String, val house: String) {
    fun cast(spell: Spell): String = spell.castBy(this)

    fun favoriteSpell(): Spell = Spell("Expelliarmus", 5)
}
