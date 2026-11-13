// RUN_PIPELINE_TILL: CODEGEN
// FILE: Node.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Node<T extends Node.NodeBuilder<?, ?, ?>> {
    public int x;
}

// FILE: test.kt

fun test() {
    Node.builder<Nothing>().x(1).build()
}

/* GENERATED_FIR_TAGS: capturedType, flexibleType, functionDeclaration, integerLiteral, javaFunction, starProjection */
