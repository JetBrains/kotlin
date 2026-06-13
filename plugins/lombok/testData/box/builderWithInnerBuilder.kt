// ISSUE: KT-74315

// FILE: Klass.java

import lombok.Builder;

@Builder
class Klass {
    public String str;

    @Builder
    static class Inner {
        public int integer;
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val innerBuilder: Klass.Inner.InnerBuilder = Klass.Inner.builder()
    val inner: Klass.Inner = innerBuilder.integer(42).build()

    assertEquals(42, inner.integer)

    val klassBuilder: Klass.KlassBuilder = Klass.builder()
    val klass: Klass = klassBuilder.str("hello").build()

    assertEquals("hello", klass.str)
    return "OK"
}
