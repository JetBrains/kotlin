// ENABLE_SERIALIZATION
// ENABLE_LOMBOK
// WITH_STDLIB
// ISSUE: KT-90516

import lombok.Builder
import lombok.Singular

// The builder items (with their `@Builder.Default` and `@Singular` annotations) are computed
// while the supertypes of the builder class are still being resolved
@Builder
class Dog(
    @Builder.Default val name: String = "Rex",
    @field:Builder.Default val age: Int = 3,
    @Singular val toys: List<String>,
) {
    class DogBuilder
}

fun box(): String {
    val dog = Dog.builder().toy("Ball").build()
    if (dog.name != "Rex") return "Fail name: ${dog.name}"
    if (dog.age != 3) return "Fail age: ${dog.age}"
    if (dog.toys != listOf("Ball")) return "Fail toys: ${dog.toys}"
    val other = Dog.builder().name("Max").age(5).build()
    return if (other.name == "Max" && other.age == 5) "OK" else "Fail other"
}
