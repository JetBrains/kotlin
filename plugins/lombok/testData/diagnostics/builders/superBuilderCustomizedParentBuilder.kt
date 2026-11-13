// A manually declared (customized) super builder of `Parent` is filtered out by `getBuilderNames(..., filterExistingNames = true)`
// in `SuperBuilderGenerator.completeBuilder`, so `ChildBuilder` gets `Any` as a supertype and loses `a(..)` and `twice(..)`.
// Real Lombok makes `ChildBuilder` extend the customized `ParentBuilder<C, B>`.

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
    Child.builder().b(1).<!UNRESOLVED_REFERENCE!>a<!>("1").twice("2").build()
}
