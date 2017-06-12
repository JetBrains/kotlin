// EXPECTED_REACHABLE_NODES: 497
package foo

var c0 = 0
var c1 = 0
var c2 = 0

class A() {
    var p = 0
    operator fun divAssign(a: Int) {
        c1++;
    }
    operator fun times(a: Int): A {
        c2++;
        return this;
    }
}

var a: A = A()
    get() {
        c0++
        return field
    }

fun box(): String {

    a /= 3
    if (c0 != 1) {
        return "1"
    }
    if (c1 != 1) {
        return "2"
    }
    a *= 3
    if (c0 != 2) {
        return "3"
    }
    if (c2 != 1) {
        return "4"
    }
    return "OK"
}