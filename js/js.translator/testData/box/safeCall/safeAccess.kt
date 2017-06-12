// EXPECTED_REACHABLE_NODES: 489
package foo

class A() {
    var c = 3
}

fun box(): String {
    var a1: A? = A()
    var a2: A? = null
    a1?.c = 4
    a2?.c = 5
    if (a1?.c != 4) {
        return "fail1"
    }
    a2 = a1
    a2?.c = 5

    if (a2?.c != 5) return "fail2"
    if (a1?.c != 5) return "fail3"

    return "OK"
}