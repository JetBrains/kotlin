// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91522
// The customized `ChildBuilder` is an existing class, so `completeBuilder` never runs for it and it doesn't get
// `Parent.ParentBuilder<C, B>` as a supertype (real Lombok adds it), so `a(..)` is unresolved.
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent {
    public String a;
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public int b;

    public static abstract class ChildBuilder<C extends Child, B extends ChildBuilder<C, B>> {
        public B doubled(int value) {
            return b(value * 2);
        }
    }
}

// FILE: test.kt

fun test() {
    // Should become green after fix of KT-91522
    Child.builder().<!UNRESOLVED_REFERENCE!>a<!>("a").doubled(2).build()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType, starProjection, stringLiteral */
