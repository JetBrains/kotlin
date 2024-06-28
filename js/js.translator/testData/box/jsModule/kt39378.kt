// EXPECTED_REACHABLE_NODES: 1238

// MODULE: lib
// FILE: l.kt

package l

external interface E {
    var ss: Array<String> // Effectively external property.
}

external fun ee(): E

fun foo(): String {

    val e = ee()

    var result =  ""

    for (s in e.ss) {
        result += s
    }

    return result
}

// MODULE: main(lib)
// FILE: m.kt

import l.*

fun box(): String {
    return foo()
}
