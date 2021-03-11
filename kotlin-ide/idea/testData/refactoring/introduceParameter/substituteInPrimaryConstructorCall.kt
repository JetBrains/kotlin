// WITH_DEFAULT_VALUE: false
// TARGET:
open class A(val a: Int) {
    constructor(): this(1)

    fun foo(): Int {
        return (<selection>a + 1</selection>) / 2
    }
}

class B: A(1) {

}

class C: A {
    constructor(n: Int): super(n + 1)
}

fun test() = A(1)