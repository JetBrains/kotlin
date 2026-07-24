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


// FILE: test.kt

fun box(): String {
    val userBuilder = User.builder()
        .created(10)
        .name("John")
        .age(42)

    val user = userBuilder.build()
    return if (user.created == 10 && user.name == "John" && user.age == 42) {
        "OK"
    } else {
        "Error: $user"
    }
}
