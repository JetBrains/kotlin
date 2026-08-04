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

class ConstructorExample(val str: String, val int: Int) {
    @Builder
    constructor(str: String) : this(str, -1)

    @Builder
    constructor(int: Int) : this("empty", int)
}

fun box(): String {
    val obj = ConstructorExample.builder() // The builder for first constructor should be chosen
        .str("str")
        .build()

    assertEquals("str", obj.str)
    assertEquals(-1, obj.int)

    // Make sure the offset-as-id approach is also robust with classes from libraries
    val obj2 = ConstructorExampleLibrary.builder().int(42).build()

    assertEquals("empty", obj2.str)
    assertEquals(42, obj2.int)

    return "OK"
}
