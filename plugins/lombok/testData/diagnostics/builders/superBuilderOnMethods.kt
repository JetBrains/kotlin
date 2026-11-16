// RUN_PIPELINE_TILL: FRONTEND
// FILE: User.java

import lombok.SuperBuilder;

public class User {
    public String name;
    public int age;

    @SuperBuilder
    public static User create(String name, int age) {
        return new User(name, age);
    }

    private User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: test.kt

fun foo() {
    val userBuilder: User.<!UNRESOLVED_REFERENCE!>UserBuilder<!> = User.<!UNRESOLVED_REFERENCE!>builder<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, localProperty, propertyDeclaration */
