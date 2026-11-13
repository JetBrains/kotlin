// ISSUE: KT-91488
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent {
    private String a;
}

// FILE: ChildA.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ChildA extends Parent {
}

// FILE: ChildB.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class ChildB extends Parent {
}

// FILE: test.kt

fun box(): String {
    val siblingBuilder = ChildA.builder()

    val childB = ChildB.builder()
        .a("1")
        .build()
    return "OK"
}
