// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder
import lombok.NoArgsConstructor

open class Animal

// The callable names of the builder class are requested while the supertypes are still being resolved. Computing
// them must not generate the constructors of `Dog`: the no-args one looks up the superclass constructor
@Builder
@NoArgsConstructor(force = true)
class Dog(val name: String) : Animal() {
    class DogBuilder
}

fun box(): String = if (Dog.builder().name("Rex").build().name == "Rex") "OK" else "Fail"
