// EXPECTED_REACHABLE_NODES: 510
package foo

object A {
    object query {
        val status = "complete"
    }
}

object B {
    private val ov = "d"
    object query {
        val status = "complete" + ov
    }
}

class C {
    companion object {
        fun ov() = "d"
    }
    object query {
        val status = "complete" + ov()
    }
}

fun box(): String {
    var result = A.query.status
    if (result != "complete") return "fail1: $result"

    result = B.query.status
    if (result != "completed") return "fail2: $result"

    result = C.query.status
    if (result != "completed") return "fail3: $result"

    return "OK"
}

