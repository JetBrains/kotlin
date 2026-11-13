// RUN_PIPELINE_TILL: CODEGEN
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent<T> {
    public String a;
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent<Child.ChildBuilder<?, ?>> {
    public int b;
}

// FILE: test.kt

fun test() {
    Child.builder().a("1").b(2).build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, starProjection, stringLiteral */
