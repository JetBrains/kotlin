// FIR_DUMP
// DUMP_KT_IR

import lombok.Builder
import lombok.AccessLevel
import kotlin.test.assertEquals

@Builder(toBuilder = true)
class User(val name: String, val age: Int, val info: String?)

// The builder is the annotated class's own business: `builder()`, the builder class and everything on it are
// private, and the class still has to be able to build itself with them. Java gets this for free - a class may
// reach a private member of its own nested class - while Kotlin's `private` inside the builder class would have
// meant that class alone, leaving `x(...)` and `build()` out of reach here (KT-89027).
@Builder(access = AccessLevel.PRIVATE)
class AccessLevelPrivate(val x: Int) {
    fun rebuild(newX: Int): AccessLevelPrivate = builder().x(newX).build()

    companion object {
        fun makeInternally(x: Int): AccessLevelPrivate = builder().x(x).build()
    }
}

fun box(): String {
    val user = User.builder()
        .name("John")
        .age(42)
        .info(null)
        .build()
    assertEquals("John", user.name)
    assertEquals(42, user.age)

    val user2 = User("Sarah", 30, null)
    val builder2 = user2.toBuilder()
    val user3 = builder2.build()
    assertEquals(user2.name, user3.name)
    assertEquals(user2.age, user3.age)
    assertEquals(user2.info, user3.info)

    assertEquals(7, AccessLevelPrivate.makeInternally(7).x)
    assertEquals(8, AccessLevelPrivate.makeInternally(7).rebuild(8).x)

    return "OK"
}
