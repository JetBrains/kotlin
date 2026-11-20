// RUN_PIPELINE_TILL: CODEGEN
// FILE: Base.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Base<T> {
    public String base;
}

// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent extends Base<Child.ChildBuilder<?, ?>> {
    public String parent;
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent {
    public String child;
}

// FILE: test.kt

fun test() {
    Child.builder().child("c").parent("p").base("b").build()
    Parent.builder().parent("p").base("b").build()
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, javaFunction, starProjection, stringLiteral */
