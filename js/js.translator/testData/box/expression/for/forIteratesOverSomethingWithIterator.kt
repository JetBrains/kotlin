// EXPECTED_REACHABLE_NODES: 500
package foo

class Iter(val upper: Int) {
    var count: Int = 0
    operator fun hasNext(): Boolean = count < upper
    operator fun next(): Int = count++
}

class A(val upper: Int) {
    operator fun iterator(): Iter = Iter(upper)
}

fun box(): String {
    var n = 0
    for(i in A(10)) {
        n++
    }
    assertEquals(10, n)

    return "OK"
}