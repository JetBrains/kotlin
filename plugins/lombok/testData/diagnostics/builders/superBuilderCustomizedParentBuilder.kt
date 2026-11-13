// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91508
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent {
    public String a;

    public static abstract class ParentBuilder<C extends Parent, B extends ParentBuilder<C, B>> {
        public B twice(String value) {
            return a(value + value);
        }
    }
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public int b;
}

// FILE: test.kt

fun test() {
    Parent.builder().a("1").twice("2").build()
    Child.builder().b(1).a("1").twice("2").build()
}

/* GENERATED_FIR_TAGS: capturedType, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaType,
starProjection, stringLiteral */
