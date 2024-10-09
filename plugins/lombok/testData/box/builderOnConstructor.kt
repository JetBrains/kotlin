// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-71547

// FILE: User.java

import lombok.Builder;
import lombok.Data;

@Data
public class User {
    private String name;
    private int age;

    @Builder
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // This builder is not generated for this constructor because it has the same name and it's overlapped by the first constructor
    @Builder
    public User(String name) {
        this.name = name;
        this.age = -1;
    }
}

// FILE: test.kt

fun box(): String {
    val userBuilder = User.builder()
        .name("John")
        .age(42)

    val user = userBuilder.build()
    return if (user.name == "John" && user.age == 42) {
        "OK"
    } else {
        "Error: $user"
    }
}
