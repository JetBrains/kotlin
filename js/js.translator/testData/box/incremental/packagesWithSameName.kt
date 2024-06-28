// NO_COMMON_FILES
// EXPECTED_REACHABLE_NODES: 1283
// MODULE: lib
// FILE: a.kt
package a.p

fun foo() = "foo"

// FILE: b1.kt
// RECOMPILE
package b.p

fun bar() = "bar"

// FILE: b2.kt
package b.p

fun baz() = "baz"

// MODULE: main(lib)
// FILE: main.kt
import a.p.*
import b.p.*

fun box(): String {
    val r = foo() + bar() + baz()

    if (r != "foobarbaz") return "fail: $r"

    return "OK"
}

