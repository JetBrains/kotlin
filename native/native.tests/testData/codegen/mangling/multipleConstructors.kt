// MODULE: lib
// FILE: lib.kt

public fun <T> mangle(l: T) = "no constructors $l"

public fun <T> mangle(l: T) where T: Comparable<T> = "single constructor $l"

public fun <T> mangle(l: T) where T: Comparable<T>, T: Number = "two constructors $l"

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    val any = mapOf<Float, Float>()
    val comparable = "some string"
    val number = 17

    assertEquals("no constructors {}", mangle(any))
    assertEquals("single constructor some string", mangle(comparable))
    assertEquals("two constructors 17", mangle(number))

    return "OK"
}