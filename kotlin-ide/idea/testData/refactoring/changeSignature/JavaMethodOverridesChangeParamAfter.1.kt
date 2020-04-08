open class X: A() {
    fun foo(x: Int): String? {
        return super.foo(x) + 1
    }
}

open class Y: B() {
    fun foo(x: Int): String? {
        return x.length * 2
    }
}

open class Z: X() {
    fun foo(x: Int): String? {
        return x.length
    }
}

fun test() {
    A().foo("")
    B().foo("")
    X().foo("")
    Y().foo("")
    Z().foo("")
}