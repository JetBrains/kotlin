// ISSUE: KT-91715
// RUN_PIPELINE_TILL: FRONTEND
// FILE: Base.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public interface Base {
}

// FILE: Derived.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Derived implements Base {
    private String some;
}

// FILE: test.kt

fun test() {
    Derived.builder().some("").build()
    // Should be error: Lombok generates no static `builder()` for an interface
    Base.<!UNRESOLVED_REFERENCE!>builder<!>()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, javaFunction, starProjection, stringLiteral */
