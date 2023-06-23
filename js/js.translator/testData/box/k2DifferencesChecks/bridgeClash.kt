// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/bridgeClash.fir.kt
// WITH_STDLIB
interface I {
    fun foo()
}

interface J {
    @JsName("bar")
    fun foo()
}

interface K : I, J {
    override fun foo()
}

interface L : K {
    override fun foo()

    fun bar()
}


fun box() = "OK".also { foo() }
