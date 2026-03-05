package cats

import example.GenerateCompanion

/**
 * A cat house that holds a Lombok-powered [Cat].
 * Annotated with [GenerateCompanion] so KAPT generates:
 * - `CatHouseHelper.java` — a Java factory with `create()` (used from [main])
 * - `CatHouseExtensions.kt` — a Kotlin extension with `describe()`
 *
 * This class ties lombok and KAPT together: the KAPT-generated helper creates a [CatHouse],
 * and the [CatHouse] itself uses the Lombok-generated [Cat].
 */
@GenerateCompanion
class CatHouse {
    var cat: Cat = adoptCat()
    var capacity: Int = 1
}

fun main() {
    // KAPT-generated Java helper creates a CatHouse (verifies Kotlin sees KAPT-generated Java)
    val house = CatHouseHelper.create()

    // Lombok-generated accessors work on the Cat inside
    house.cat.name = "Luna"
    house.cat.lives = 9
    println("Cat: ${house.cat}")

    // KAPT-generated Kotlin extension
    println(house.describe())

    // Cross-language call: Java -> Kotlin -> Java (lombok)
    JavaUsage.crossLanguageCall()
}