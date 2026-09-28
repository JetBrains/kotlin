// ISSUE: KT-71547

// FILE: User.java

import lombok.Builder;

public class User {
    String name;
    int age;

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

    public static void testBuilder() {
        User user = User.builder().name("name").age(42).build();
        if (user.age != 42) throw new AssertionError();
    }
}

// FILE: User2.java

import lombok.Builder;

public class User2 {
    String name;
    int age;

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

    public static void testBuilder() {
        User2 user2 = User2.builder().name("name2").age(5).build();
        if (user2.age != -1) throw new AssertionError();
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    // Correct
    User.testBuilder()
    val user = User.builder().name("name").age(42).build()
    assertEquals(42, user.age)
    assertEquals("name", user.name)

    User2.testBuilder()
    // Green code, although the `age` becomes initialized by the first constructor that's looks awkward.
    // It's a way how Lombok works in case of builder clashing
    val user2 = User2.builder().name("name2").age(5).build()
    assertEquals(-1, user2.age)
    assertEquals("name2", user2.name)

    return "OK"
}
