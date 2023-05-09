// EXPECTED_REACHABLE_NODES: 1283
// KJS_WITH_FULL_RUNTIME

// MODULE: AT
// FILE: at.kt
package foo

fun ooo(): String = "O"
fun kkk(): String = "Fail AT"

// MODULE: A(AT)
// FILE: a.kt
package bar

import foo.*

fun o(): String = ooo()

// MODULE: BT
// FILE: bt.kt
package foo

fun ooo(): String = "Fail BT"
fun kkk(): String = "K"

// MODULE: B(BT)
// FILE: b.kt
package bar

import foo.*

fun k(): String = kkk()

// MODULE: main(A, B)
// FILE: main.kt
package main

import bar.*

fun box(): String {
    return o() + k()
}
