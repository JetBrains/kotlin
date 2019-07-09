// EXPECTED_REACHABLE_NODES: 1281
// FILE: 1.kt

package o

import I

inline fun run(): String {
    return object : I {
        override fun run() = "O"
    }.run()
}


// FILE: 2.kt

package k

import I

inline fun run(): String {
    return object : I {
        override fun run() = "K"
    }.run()
}

// FILE: 3.kt

fun ok() = o.run() + k.run()

// FILE: main.kt
// RECOMPILE
interface I {
    fun run(): String
}

fun box(): String {

    if (ok() != "OK") return "fail"


    return o.run() + k.run()
}