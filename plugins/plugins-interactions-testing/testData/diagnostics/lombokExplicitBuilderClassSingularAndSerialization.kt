// ENABLE_LOMBOK
// ENABLE_SERIALIZATION
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder
import lombok.Singular

@Builder
class Dog(@Singular val toys: List<String>) {
    class DogBuilder

    companion object
}

fun test() = Dog.builder().toy("Ball").toys(listOf("Bone")).clearToys().build()
