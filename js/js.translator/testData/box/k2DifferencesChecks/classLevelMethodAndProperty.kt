// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/classLevelMethodAndProperty.fir.kt
// WITH_STDLIB
package foo

class A {
    fun bar() = 23

    val bar = 23
}


fun box() = "OK"
