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

// FILE: GrandChild.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class GrandChild extends Child {
    public long c;
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val grandChild = GrandChild.builder().twice("x").b(1).c(2L).build()
    assertEquals("xx", grandChild.a)
    assertEquals(1, grandChild.b)
    assertEquals(2L, grandChild.c)

    return "OK"
}
