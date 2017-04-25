// EXPECTED_REACHABLE_NODES: 493
// MODULE: lib
// FILE: lib.kt

package foo

inline fun bar(x: Int = 0) = x + 10


// MODULE: main(lib)
// FILE: main.kt

package foo

// CHECK_NOT_CALLED: bar

fun box(): String {
    assertEquals(10, bar())
    assertEquals(11, bar(1))

    return "OK"
}