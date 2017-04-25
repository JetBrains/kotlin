// EXPECTED_REACHABLE_NODES: 499
// FILE: a.kt
package foo

import bar.*
import bar.Some.importedFunc

fun box(): String {
    Some.justFunc()
    importedFunc()
    assertEquals("justFunc();importedFunc();", log)
    return "OK"
}

// FILE: b.kt
package bar

var log = ""

object Some {
    fun justFunc() {
        log += "justFunc();"
    }

    fun importedFunc() {
        log += "importedFunc();"
    }
}