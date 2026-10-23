// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90086

// FILE: test.kt

import lombok.ToString

@ToString
open class Animal(val species: String)

// `callSuper` is left to `lombok.config`, which says `call`.
@ToString
class Dog(species: String, val name: String) : Animal(species)

fun box(): String {
    val result = Dog("Canis", "Rex").toString()
    return if (result == "Dog(super=Animal(species=Canis), name=Rex)") "OK" else "Fail: $result"
}

// FILE: lombok.config

lombok.toString.callSuper=call
