// FIR_IDENTICAL

// FILE: Some.java

import lombok.experimental.SuperBuilder;

@SuperBuilder
class Some {
    public int x;

    @SuperBuilder // Incorrect using of annotation, but we have to make sure the compiler doesn't crash
    public Some(int x) {}
}

// FILE: test.kt

fun test() {
    // Despite the incorrect code, Lombok generates builder class for `Some` class declration with `@SuperBuilder`
    val someBuilder: Some.SomeBuilder<*, *> = Some.builder()
    val some: Some = someBuilder.x(42).build()
}
