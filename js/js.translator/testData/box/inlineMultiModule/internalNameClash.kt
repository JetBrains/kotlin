// EXPECTED_REACHABLE_NODES: 1284
// MODULE: lib1
// FILE: lib1.kt
package lib1

fun foo() = "lib1"

inline fun bar1() = foo()

// MODULE: lib2
// FILE: lib2.kt
package lib2

fun foo() = "lib2"

inline fun bar2() = foo()

// MODULE: main(lib1, lib2)
// FILE: main.kt

import lib1.bar1
import lib2.bar2

fun box(): String {
    val a = bar1()
    if (a != "lib1") return "fail1: $a"

    val b = bar2()
    if (b != "lib2") return "fail2: $b"

    return "OK"
}
