// FIR_IDENTICAL
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

    @Builder
    public User(String name) {
        this.name = name;
        this.age = -1;
    }
}

// FILE: User2.java

import lombok.Builder;
import lombok.Data;

@Data
public class User2 {
    private String name;
    private int age;

    @Builder
    public User2(String name) {
        this.name = name;
        this.age = -1;
    }

    @Builder
    public User2(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: test.kt

fun test() {
    // Correct
    val user = User.builder().name("name").age(42).build()

    // Incorrect, `age` is not accessible because the first constructor always wins
    val user2 = User2.builder().name("name2").<!UNRESOLVED_REFERENCE!>age<!>(5).build()
}
