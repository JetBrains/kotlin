// EXPECTED_REACHABLE_NODES: 1281
// ES_MODULES
// CALL_MAIN

// FILE: ok.kt

package ok

var ok: String = "fail"

// FILE: 1.kt
// RECOMPILE

package a.a

import ok.*

fun main(args: Array<String>) {
    ok = "fail: b.b"
}

// FILE: 0.kt

package b

import ok.*

fun main(args: Array<String>) {
    ok = "fail: b"
}

// FILE: 2.kt

package a

import ok.*

fun main(args: Array<String>) {
    ok = "OK"
}

// FILE: 3.kt

package a.b

import ok.*

fun main(args: Array<String>) {
    ok = "fail: a.b"
}

// FILE: main.kt

import ok.*

fun box() = ok