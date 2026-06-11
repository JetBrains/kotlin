// FIR_DUMP
// FILE: User.java

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class User {
  @Builder.Default private int created = 0;
  private String name;
  private int age;
}

// FILE: UserKotlin.kt

import lombok.Builder;

@Builder(toBuilder = true)
class UserKotlin(val name: String, val age: Int)

// FILE: test.kt

fun box(): String {
    val user = User.builder()
        .created(10)
        .name("John")
        .age(42)
    assertEquals(10, user.created)
    assertEquals("John", user.name)
    assertEquals(42, user.age)

    val userKotlin = UserKotlin.builder()
        .name("John")
        .age(42)
    assertEquals(user.name, userKotlin.name)
    assertEquals(user.age, userKotlin.age)

    return "OK"
}
