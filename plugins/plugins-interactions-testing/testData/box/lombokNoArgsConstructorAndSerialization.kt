// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90086

import lombok.NoArgsConstructor

open class Animal

// The superclass constructor to delegate to is looked up while the supertypes are still being resolved
@NoArgsConstructor(force = true)
class Dog(val name: String) : Animal()

fun box(): String {
    Dog()
    return if (Dog("Rex").name == "Rex") "OK" else "Fail"
}
