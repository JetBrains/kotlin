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
class X2 extends X1 {
    public int field2;
}

// FILE: Y.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class Y extends X1 {
    public int y;
}

// FILE: test.kt

fun test() {
    Y.builder().y(0).<!UNRESOLVED_REFERENCE!>field1<!>("str1").field2(1).build()
    X1.builder().field1("str1").<!UNRESOLVED_REFERENCE!>field2<!>(1).build()
    X2.builder().field2(2).<!UNRESOLVED_REFERENCE!>field1<!>("str2").build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, starProjection, stringLiteral */
