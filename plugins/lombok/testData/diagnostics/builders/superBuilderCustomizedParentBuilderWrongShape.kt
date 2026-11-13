// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91522
// Incorrect code: Lombok requires `ParentBuilder<C extends Parent, B extends ParentBuilder<C, B>>`.
// The compiler must not crash on a child whose super builder has an unexpected number of type parameters.

// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent {
    public String a;

    public static abstract class ParentBuilder {
    }
}

// FILE: GenericParent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class GenericParent<T> {
    public T value;

    public static abstract class GenericParentBuilder<C extends GenericParent<?>, B extends GenericParentBuilder<C, B>> {
    }
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public int b;
}

// FILE: GenericChild.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class GenericChild extends GenericParent<String> {
    public int b;
}

// FILE: test.kt

fun test() {
    Child.builder().b(1).<!FUNCTION_EXPECTED!>a<!>("1").<!UNRESOLVED_REFERENCE!>build<!>()
    GenericChild.builder().b(1).value("1").<!UNRESOLVED_REFERENCE!>build<!>()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, integerLiteral, javaFunction, starProjection, stringLiteral */
