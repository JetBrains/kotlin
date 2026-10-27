// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90086

import lombok.EqualsAndHashCode

interface Pet

@EqualsAndHashCode
class Cat(val name: String) : Pet

@EqualsAndHashCode
open class Animal(val species: String)

@EqualsAndHashCode(callSuper = true)
class Dog(species: String, val name: String) : Animal(species)

// The user-declared `equals` overload is inspected while the supertypes are still being resolved
@EqualsAndHashCode
class Fish(val name: String) : Pet {
    fun equals(other: Fish): Boolean = true
}

fun box(): String {
    if (Cat("Tom") != Cat("Tom")) return "Fail: Cat == Cat"
    if (Cat("Tom") == Cat("Kitty")) return "Fail: Cat != Cat"
    if (Dog("Canis", "Rex") != Dog("Canis", "Rex")) return "Fail: Dog == Dog"
    if (Dog("Canis", "Rex") == Dog("Canis", "Max")) return "Fail: Dog != Dog"
    if (Animal("Canis") == Dog("Canis", "Rex")) return "Fail: Animal != Dog"
    if (Dog("Canis", "Rex") == Animal("Canis")) return "Fail: Dog != Animal"
    if (Fish("Nemo") != Fish("Nemo")) return "Fail: Fish == Fish"
    if (Fish("Nemo") == Fish("Dory")) return "Fail: Fish != Fish"
    if (!Fish("Nemo").equals(Fish("Dory"))) return "Fail: user-declared Fish.equals"
    return "OK"
}
