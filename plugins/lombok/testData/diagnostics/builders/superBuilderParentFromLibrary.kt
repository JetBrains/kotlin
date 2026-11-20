// ISSUE: KT-91523
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: lib
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent {
    public String a;
}

// MODULE: main(lib)
// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public int b;
}

// FILE: test.kt

fun test() {
    // The code should be green after fix of KT-91523
    Child.builder().b(1).<!UNRESOLVED_REFERENCE!>a<!>("1").build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, starProjection, stringLiteral */
