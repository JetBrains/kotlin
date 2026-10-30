// ISSUE: KT-90508
import lombok.Builder
import kotlin.test.assertEquals

@Builder
class User(name: String) {
    var <!BUILDER_PROPERTY_NOT_INCLUDED("greeting; a Kotlin class builds out of its primary constructor''s value parameters. To make it a builder field, declare it in the primary constructor")!>greeting<!> = "Hello, $name"
}

// The body property shares its name with the parameter, so `toBuilder()` can read it
@Builder(toBuilder = true)
class SameName(greeting: String) {
    var greeting = greeting
}

fun box(): String {
    val user = User.builder().name("Alice").build()
    assertEquals("Hello, Alice", user.greeting)

    val original = SameName.builder().greeting("Hi").build()
    original.greeting = "Changed"
    val copy = original.toBuilder().build()
    assertEquals("Changed", copy.greeting)

    return "OK"
}
