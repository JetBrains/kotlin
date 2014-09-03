package foo

class Iter(val upper: Int) {
    var count: Int = 0
    fun hasNext(): Boolean = count < upper
    fun next(): Int = count++
}

class A(val upper: Int) {
    fun iterator(): Iter = Iter(upper)
}

fun box(): String {
    var n = 0
    for(i in A(10)) {
        n++
    }
    assertEquals(10, n)

    return "OK"
}