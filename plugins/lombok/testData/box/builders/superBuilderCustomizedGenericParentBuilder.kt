// ISSUE: KT-91508
// FILE: Parent.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Parent<T> {
    public T value;

    public static abstract class ParentBuilder<T, C extends Parent<T>, B extends ParentBuilder<T, C, B>> {
        public B valueOf(T v) {
            return value(v);
        }
    }
}

// FILE: Child.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class Child extends Parent<String> {
    public int b;
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val parent: Parent<Int> = Parent.builder<Int>().valueOf(42).build()
    assertEquals(42, parent.value)

    val child: Child = Child.builder().valueOf("v").b(1).build()
    assertEquals("v", child.value)
    assertEquals(1, child.b)

    return "OK"
}
