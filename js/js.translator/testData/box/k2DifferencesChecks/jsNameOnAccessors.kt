// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/jsNameOnAccessors.fir.kt
// WITH_STDLIB
package foo

class A {
    var x: Int
        @JsName("xx") get() = 0
        @JsName("xx") set(value) {}
}


fun box() = "OK"
