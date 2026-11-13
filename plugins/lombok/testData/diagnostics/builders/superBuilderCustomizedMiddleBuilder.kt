// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91522
// The customized `ParentBuilder` is an existing class, so `completeBuilder` never runs for it and it doesn't get
// `GrandParent.GrandParentBuilder<C, B>` as a supertype (real Lombok adds it), so `g(..)` is unresolved on both builders.
// FILE: GrandParent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class GrandParent {
    public String g;
}

// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent extends GrandParent {
    public String p;

    public static abstract class ParentBuilder<C extends Parent, B extends ParentBuilder<C, B>> {
        public B both(String value) {
            return g(value).p(value);
        }
    }
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public String c;
}

// FILE: test.kt

fun test() {
    // Should become green after fix of KT-91522
    Parent.builder().<!UNRESOLVED_REFERENCE!>g<!>("g").p("p").build()
    Child.builder().<!UNRESOLVED_REFERENCE!>g<!>("g").p("p").c("c").build()
    Child.builder().both("x").c("c").build()
}

/* GENERATED_FIR_TAGS: capturedType, flexibleType, functionDeclaration, javaFunction, javaType, starProjection,
stringLiteral */
