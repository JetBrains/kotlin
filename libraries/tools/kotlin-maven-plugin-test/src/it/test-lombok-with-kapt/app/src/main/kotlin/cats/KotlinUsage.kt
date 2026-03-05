package cats

/**
 * Kotlin code that uses Lombok-generated accessors on [Cat] and [CatOwner] (Java classes).
 * This works because the Kotlin lombok compiler plugin understands Lombok annotations.
 */
fun adoptCat(): Cat {
    val cat = Cat()
    cat.name = "Mittens"
    cat.lives = 7
    cat.isPurring = true
    return cat
}

/**
 * A Kotlin class called from Java ([JavaUsage.crossLanguageCall]) to verify cross-language compilation.
 */
class CatClinic {
    fun checkup() {
        val owner = CatOwner()
        owner.name = "Alice"
        owner.catCount = 3
        println("$owner brings cats to the clinic")

        val shelter = CatShelter()
        val shelterName: String = shelter.shelterName  // @NotNull — no null check needed
        val found: Cat? = shelter.findCatByName("Ghost")  // @Nullable — correctly typed as Cat?
        println("Shelter: $shelterName, found: $found")
    }
}