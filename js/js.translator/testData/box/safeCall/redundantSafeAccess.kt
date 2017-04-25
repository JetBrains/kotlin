// EXPECTED_REACHABLE_NODES: 500
package foo

class A(val x: String) {
    fun foo() = x
}

fun box(): String {
    var b: A = createWrongObject()
    if (b?.x != null) return "fail1: ?."
    if (b?.foo() != null) return "fail1a: ?."
    try {
        println(b!!.x)
        return "fail1: !!"
    }
    catch (e: NullPointerException) {
        // It's expected
    }

    b = A("OK")
    if (b?.x != "OK") return "fail2: ?."
    if (b?.foo() != "OK") return "fail2a: ?."
    try {
        if (b!!.x != "OK") return "fail2: !!"
    }
    catch (e: NullPointerException) {
        return "fail2a: !!"
    }

    return "OK"
}

external fun createWrongObject(): A