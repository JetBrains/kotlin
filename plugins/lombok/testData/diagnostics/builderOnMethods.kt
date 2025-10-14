// FIR_IDENTICAL
// ISSUE: KT-71893

// FILE: User.java

import lombok.Builder;

public class User {
    private String name;
    private int age;

    @Builder
    public static void create(String name, int age) {
        return User(name, age);
    }

    @Builder
    public static void create(String name) {
        return User(name, -1);
    }

    private User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: User2.java

import lombok.Builder;

public class User2 {
    private String name;
    private int age;

    @Builder
    public static void create(String name) {
        return User2(name, -1);
    }

    @Builder
    public static void create(String name, int age) {
        return User2(name, age);
    }

    private User2(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: Foo.java

public class Foo {
    @Builder(builderMethodName = "arrayBuilder") // Incorrect, because builder name is inferred from return type and it's an invalid identifier `Char[]Builder`
    char[] initWithCharArray(char[] chars) { return chars; }
}

// FILE: test.kt

fun test() {
    // Correct
    val user = User.<!UNRESOLVED_REFERENCE!>builder<!>().name("name").age(42).build()

    // Incorrect, `age` is not accessible because the first method always wins
    val user2 = User2.<!UNRESOLVED_REFERENCE!>builder<!>().name("name2").age(5).build()

    val foo = Foo()
    foo.<!UNRESOLVED_REFERENCE!>arrayBuilder<!>().chars(arrayOf('a', 'b', 'c')).build()
}
