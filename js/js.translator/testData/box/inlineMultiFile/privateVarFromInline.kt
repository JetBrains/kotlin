// EXPECTED_REACHABLE_NODES: 1282
// FILE: f.kt
package foo

/**
 * KT-33334
 */

internal fun foo(c: C) = c.getter()

internal fun bar(c: C) { c.setter("OK") }

// FILE: c.kt
package foo

var log = ""

internal class C {
    private var tokenPosition: String = "FAIL"
        get() { logg("G"); return field  }
        set(value) { logg("S"); field = value }

    private fun logg(s: String) { log += s }

    internal inline fun setter(s: String) { tokenPosition = s }
    internal inline fun getter(): String = tokenPosition
}

// FILE: m.kt
package foo

fun box(): String {

    val c = C()

    bar(c)
    val r = foo(c)
    if (log != "SG") return "FAIL1: $log"

    return r
}