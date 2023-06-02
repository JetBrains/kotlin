// EXPECTED_REACHABLE_NODES: 1291
// See KT-6201
package foo

import kotlin.reflect.KProperty

var log = ""

open class Mixin {
    init {
        log = "mixin"
    }
}

fun initCall(): Number {
    log = "initCall"
    return 2
}

object O1 {
    init {
        log = "O1 init"
    }
}

object O2 {
    init {
        initCall()
    }
}

object O3 : Mixin()

object O4 {
    val someValue = initCall()
}

object O5 {
    val someValue = O3.also { log = "O5 also" }
}

fun box(): String {
    if (log != "") return "Fail: something was initialized before any object was used"
    O1
    if (log != "O1 init") return "Fail: O1 didn't initialized lazy"
    O2
    if (log != "initCall") return "Fail: O2 didn't initialized lazy"
    O3
    if (log != "mixin") return "Fail: O3 didn't initialized lazy"
    O4
    if (log != "initCall") return "Fail: O4 didn't initialized lazy"
    O5
    if (log != "O5 also") return "Fail: O5 didn't initialized lazy"
    return "OK"
}