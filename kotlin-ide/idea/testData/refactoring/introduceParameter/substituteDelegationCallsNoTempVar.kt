// WITH_DEFAULT_VALUE: false
class T(val t: Int)

open class A {
    constructor(): this(1)

    constructor(a: Int) {
        val x = <selection>T(a + 1)</selection>.t / 2
    }
}

class B: A {
    constructor(n: Int): super(n + 1)
}

class C: A(1) {

}

fun test() {
    A(2)
}