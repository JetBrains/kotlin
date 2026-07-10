// FIR_DUMP

import lombok.Builder

@Builder(toBuilder = true)
class User(val name: String, val age: Int, val info: String?)

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

    return "OK"
}
