// EXPECTED_REACHABLE_NODES: 490
package foo

class A() {
    var c = 3
}

fun A.i(): Int {
    c = c + 1
    return c
}

fun box(): String {
    var a1: A? = A()
    var a2: A? = null
    if (a1?.i() != 4) {
        return "1";
    }
    if (a1?.c != 4) {
        return "2";
    }
    if (a2?.c != null) {
        return "3";
    }
    a2?.i()
    if (a1?.c != 4) {
        return "4";
    }
    a2 = a1
    if (a2?.i() != 5) {
        return "5";
    }
    if ((a2?.i() != 6) || (a1?.c != 6)) {
        return "6"
    }
    return "OK"
}