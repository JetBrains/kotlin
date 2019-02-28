// SKIP_MINIFICATION
// Contains calls from external JS code

open class A {
    @JsName("foo")
    open protected fun foo(n: Int) = 23

    @JsName("bar")
    fun bar(n: Int) = foo(n) + 100
}

open class B {
    @JsName("foo")
    protected fun foo(n: Int) = 42

    @JsName("bar")
    open fun bar(n: Int) = 142
}

external fun createA(): A

external fun createB(): B

fun box(): String {
    val a = createA()
    if (a.bar(0) != 124) return "fail1: ${a.bar(0)}"

    val b = createB()
    if (b.bar(0) != 42) return "fail2: ${b.bar(0)}"

    return "OK"
}