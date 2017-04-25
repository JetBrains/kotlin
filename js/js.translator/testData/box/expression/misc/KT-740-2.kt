// EXPECTED_REACHABLE_NODES: 501
package foo

var c0 = 0
var c1 = 0
var c2 = 0
var c3 = 0

fun cStr(): String {
    return "${c0}${c1}${c2}${c3}"
}

fun get1(): Int {
    c3++
    return 1
}

class A() {
    var p = 0
    operator fun get(i: Int): Int {
        c1++
        return 0
    }

    operator fun set(i: Int, value: Int) {
        c2++
    }
}

val a: A = A()
    get() {
        c0++
        return field
    }

fun box(): String {
    var d = a[1]
    if (cStr() != "1100") {
        return "Fail: d = a[1], cStr(): ${cStr()}"
    }

    ++a[1]
    if (cStr() != "2310") {
        return "Fail: ++a[1], cStr(): ${cStr()}"
    }

    --a[1]
    if (cStr() != "3520") {
        return "Fail: --a[1], cStr(): ${cStr()}"
    }

    ++a[get1()]
    if (cStr() != "4731") {
        return "Fail: ++a[get1()], cStr(): ${cStr()}"
    }

    return "OK"
}