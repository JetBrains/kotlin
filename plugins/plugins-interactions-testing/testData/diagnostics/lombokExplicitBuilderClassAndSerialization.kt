// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder

@Builder
class Dog(val name: String) {
    class DogBuilder
}

fun test() = Dog.builder().name("Rex").build()
