// EXPECTED_REACHABLE_NODES: 1284
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MODULE: AT
// FILE: at.kt
package foo

internal fun qqq() = "Fail AT"

// MODULE: A(AT)
// FILE: a.kt
package foo

internal fun qqq() = "O"

fun o(): String = qqq()

// MODULE: BT
// FILE: bt.kt
package foo

internal fun qqq() = "K"

fun ik() = qqq()

// MODULE: B(BT)
// FILE: b.kt
package foo

internal fun qqq() = "Fail B"

fun k(): String = ik()

// MODULE: main(A, B)
// FILE: main.kt
package main

import foo.*

fun box(): String {
    return o() + k()
}
