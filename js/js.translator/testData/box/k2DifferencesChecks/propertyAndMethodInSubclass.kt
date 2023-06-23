// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/propertyAndMethodInSubclass.fir.kt
// WITH_STDLIB
package foo

open class Super {
    val foo = 23
}

class Sub : Super() {
    fun foo() = 42
}


fun box() = "OK".also { foo() }
