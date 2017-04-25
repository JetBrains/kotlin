// EXPECTED_REACHABLE_NODES: 527
package foo

enum class E {
    A,
    B {
        init {
            log("init B")
        }
    },
    C;

    init {
        log("init E")
    }
}

var l = ""
fun log(s: String) {
    l += s + ";"
}

fun box(): String {
    log("get Ea.A")
    E.A.toString()
    log("get E.B")
    E.B.toString()
    log("get E.C")
    E.C.toString()

    if (l != "get Ea.A;init E;init E;init B;init E;get E.B;get E.C;") return "fail: '$l'"

    return "OK"
}