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

import kotlin.test.assertEquals

fun box(): String {
    val parent = Parent.builder().twice("p").build()
    assertEquals("pp", parent.a)

    val child = Child.builder().b(1).twice("c").build()
    assertEquals("cc", child.a)
    assertEquals(1, child.b)

    val child2: Child = Child.builder().a("x").b(2).build()
    assertEquals("x", child2.a)
    assertEquals(2, child2.b)

    return "OK"
}
