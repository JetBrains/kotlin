// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87563
// No infinite recursion is expected; setters inherited through the cycle are lost.
// MUTE_LL_FIR: see superBuilderOnCycledHierarchy.kt

// FILE: X1.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X1 extends X2 {
    public String field1;
}

// FILE: X2.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X2 extends X3 {
    public int field2;
}

// FILE: X3.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X3 extends X1 {
    public long field3;
}

// FILE: test.kt

fun test() {
    X1.builder().field1("str1").<!UNRESOLVED_REFERENCE!>field2<!>(1).field3(1L).build()
    X2.builder().field2(1).<!UNRESOLVED_REFERENCE!>field3<!>(1L).field1("str1").build()
    X3.builder().field3(1L).<!UNRESOLVED_REFERENCE!>field1<!>("str1").field2(1).build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, starProjection, stringLiteral */
