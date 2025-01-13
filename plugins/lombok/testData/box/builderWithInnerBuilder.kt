// IGNORE_BACKEND_K2: ANY
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

fun box(): String {
    val innerBuilder: Klass.Inner.InnerBuilder = Klass.Inner.builder()
    val inner: Klass.Inner = innerBuilder.integer(42).build()

    if (inner.integer != 42) return "Error: $inner"

    val klassBuilder: Klass.KlassBuilder = Klass.builder()
    val klass: Klass = klassBuilder.str("hello").build()

    return if (klass.str == "hello") "OK" else "Error: $klass"
}
