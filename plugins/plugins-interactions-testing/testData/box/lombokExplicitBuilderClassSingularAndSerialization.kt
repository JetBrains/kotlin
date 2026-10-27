// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder
import lombok.Singular

@Builder
class Dog(@Singular val toys: List<String>) {
    class DogBuilder

    companion object
}

fun box(): String {
    val dog = Dog.builder().toy("Ball").toys(listOf("Bone", "Stick")).build()
    return if (dog.toys == listOf("Ball", "Bone", "Stick")) "OK" else "Fail: ${dog.toys}"
}
