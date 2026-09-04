// MODULE: lib
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


// MODULE: main(lib)
// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val userBuilder = User.builder()
        .created(10)
        .name("John")
        .age(42)

    val user = userBuilder.build()
    assertEquals(10, user.created)
    assertEquals("John", user.name)
    assertEquals(42, user.age)

    return "OK"
}
