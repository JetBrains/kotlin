// ISSUE: KT-87683

// MODULE: lib
// FILE: lib.kt

import lombok.Builder

class ConstructorExampleLibrary(val str: String, val int: Int) {
    @Builder
    constructor(int: Int) : this("empty", int)

    @Builder
    constructor(str: String) : this(str, -1)
}

// MODULE: main(lib)
// FILE: main.kt

import lombok.Builder
import kotlin.test.assertEquals

class ConstructorExample(val str: String, val int: Int) {
    @Builder
    constructor(str: String) : this(str, -1)

    @Builder
    constructor(int: Int) : this("empty", int)
}

// A constructor builder builds out of that constructor's parameters, so it works on a class with no primary
// constructor at all - the shape a class-level `@Builder` reports BUILDER_REQUIRES_PRIMARY_CONSTRUCTOR for.
class NoPrimaryConstructor {
    val y: Int

    @Builder
    constructor(x: Int) {
        y = x * 2
    }
}

fun box(): String {
    val obj = ConstructorExample.builder() // The builder for first constructor should be chosen
        .str("str")
        .build()

    assertEquals("str", obj.str)
    assertEquals(-1, obj.int)

    // Make sure resolving the entity constructor by symbol is also robust with classes from libraries
    val obj2 = ConstructorExampleLibrary.builder().int(42).build()

    assertEquals("empty", obj2.str)
    assertEquals(42, obj2.int)

    assertEquals(10, NoPrimaryConstructor.builder().x(5).build().y)

    return "OK"
}
