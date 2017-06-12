// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    var d = 0
    for (i in 1..6) {
        d += i
    }
    if (d != 21) return "fail1: $d"

    for (x in 100..199) {
        d += 1
    }
    if (d != 121) {
        return "fail2: $d"
    }

    for (y in 1..1) {
        d = 100
    }

    if (d != 100) {
        return "fail3: $d"
    }

    for (c in 100..100) {
        d += 1;
    }

    if (d != 101) {
        return "fail4: $d"
    }

    return "OK"
}
