// RUN_PIPELINE_TILL: CODEGEN
// DISABLE_JAVA_FACADE
// ISSUE: KT-87563
// No infinite recursion is expected; setters inherited through the cycle are lost.

// FILE: X.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class X extends X {
    public String field;
}

// FILE: test.kt

fun test() {
    X.builder().field("str").build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, javaFunction, starProjection, stringLiteral */
