// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    var log = ""

    var s: Any? = null
    for (t in arrayOf("1", "2", "3")) {
        fun foo() = {
            fun q() = { t }
            q()
        }

        if (s == null) {
            s = foo()
        }

        log += (s as (() -> (() -> String)))()()
    }

    if (log != "111") return "fail1: ${log}"

    s = null
    log = ""
    for (t in arrayOf("1", "2", "3")) {
        fun foo() = {
            val y = t
            fun q() = { y }
            q()
        }

        if (s == null) {
            s = foo()
        }

        log += (s as (() -> (() -> String)))()()
    }

    if (log != "111") return "fail2: ${log}"

    return "OK"
}