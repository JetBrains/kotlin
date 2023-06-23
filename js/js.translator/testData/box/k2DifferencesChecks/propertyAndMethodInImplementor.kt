// ORIGINAL: /compiler/testData/diagnostics/testsWithJsStdLib/name/propertyAndMethodInImplementor.fir.kt
// WITH_STDLIB
package foo

interface I {
    fun foo() = 23
}

class Sub : I {
    var foo = 42
}


fun box() = "OK".also { foo() }
