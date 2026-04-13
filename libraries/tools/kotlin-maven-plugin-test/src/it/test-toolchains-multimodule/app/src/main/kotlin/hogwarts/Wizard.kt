package hogwarts

class Wizard(val name: String) {
    fun cast(spell: Spell): String = "${name} casts ${spell.incantation}!"

    fun favoriteSpell(): Spell = Spell("Expelliarmus", 5)
}
