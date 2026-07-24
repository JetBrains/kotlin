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

fun box(): String {
    // Correct
    User.testBuilder()
    val user = User.builder().name("name").age(42).build()
    assertEquals(user.age, 42)
    assertEquals(user.name, "name")

    User2.testBuilder()
    // Green code, although the `age` becomes initialized by the first constructor that's looks awkward.
    // It's a way how Lombok works in case of builder clashing
    val user2 = User2.builder().name("name2").age(5).build()
    assertEquals(user2.age, -1)
    assertEquals(user2.name, "name2")

    return "OK"
}
