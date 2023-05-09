// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1336
// MODULE: lib
// FILE: lib.kt
open class A {
    private lateinit var a: Any
    open lateinit var c: Any

    fun useA() = a
    fun useC() = c

    companion object {
        lateinit var b: Any

        fun useB() = b
    }
}

object O {
    lateinit var o: Any

    fun useO() = o
}

fun <T> checkException(propertyName: String, f: () -> T) {
    try {
        f()
    }
    catch (e: kotlin.UninitializedPropertyAccessException) {
        assertEquals(e.message, "lateinit property ${propertyName} has not been initialized")
    }
}

fun runChecks() {
    checkException("a") { A().useA() }
    checkException("b") { A.b }
    checkException("b") { A.useB() }
    checkException("o") { O.o }
    checkException("o") { O.useO() }
}

// MODULE: main(lib)
// FILE: main.kt

class C: A() {
    override lateinit var c: Any
}

fun box(): String {
    runChecks()

    checkException("a") { A().useA() }
    checkException("b") { A.b }
    checkException("b") { A.useB() }
    checkException("o") { O.o }
    checkException("o") { O.useO() }

    checkException("a") { C().useA() }
    checkException("c") { C().c }
    checkException("c") { C().useC() }

    return "OK"
}
