// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/conflictingNamesFromSuperclass.fir.kt
// WITH_STDLIB
interface A {
    @JsName("foo") fun f()
}

interface B {
    @JsName("foo") fun g()
}

class C : A, B {
    override fun f() {}

    override fun g() {}
}

abstract class D : A, B

open class E {
    open fun f() {}

    open fun g() {}
}

class F : E(), A, B


fun box() = "OK"
