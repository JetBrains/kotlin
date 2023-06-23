// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/jsNameAndOverridden.fir.kt
// WITH_STDLIB
package foo

open class Super {
    fun foo() = 23
}

class Sub : Super() {
    @JsName("foo") fun bar() = 42
}


fun box() = "OK".also { foo() }
