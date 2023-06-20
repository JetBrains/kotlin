// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/topLevelMethodAndJsNameConstructor.fir.kt
// WITH_STDLIB
package foo

class A(val x: String) {
    @JsName("aa") constructor(x: Int) : this("int $x")
}

fun aa() {}


fun box() = "OK"
