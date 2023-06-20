// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/overrideOverloadedNativeFunction.fir.kt
// WITH_STDLIB
external open class A {
    open fun f(x: Int): Unit

    open fun f(x: String): Unit
}

class InheritClass : A() {
    override fun f(x: Int): Unit { }
}


fun box() = "OK"
