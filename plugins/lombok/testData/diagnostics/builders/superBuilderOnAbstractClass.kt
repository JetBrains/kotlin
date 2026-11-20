// ISSUE: KT-91715
// RUN_PIPELINE_TILL: FRONTEND
// FILE: Base.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class Base {
    private String some;
}

// FILE: Derived.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Derived extends Base {}

// FILE: test.kt

fun test() {
    Derived.builder().some("555-0100").build()
    // Should be error: Lombok generates no static `builder()` for an abstract class
    Base.<!UNRESOLVED_REFERENCE!>builder<!>()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, javaFunction, starProjection, stringLiteral */
