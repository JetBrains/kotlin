// MODULE: lib
// FILE: lib.kt

val sb = StringBuilder()

// Private classes
private open class A {
    public fun foo1() = sb.appendLine("PASS")
    internal fun foo2() = sb.appendLine("PASS")
    protected fun foo3() = sb.appendLine("PASS")
}

private class B:A() {
    fun foo4() = foo3()
}

// Private interfaces
private interface C {
    fun foo() = sb.appendLine("PASS")
}

private class D: C

fun runner() {
    B().foo1()
    B().foo2()
    B().foo4()

    D().foo()

    // Objects
    object : A(){
        fun foo4() = foo3()
    }.apply {
        foo1()
        foo2()
        foo4()
    }

    // Function local classes
    abstract class E {
        public open fun foo1() = sb.appendLine("PASS")
        internal open fun foo2() = sb.appendLine("PASS")
        protected open fun foo3() = sb.appendLine("PASS")
    }
    class F : E() {
        fun foo4() = foo3()
    }
    F().foo1()
    F().foo2()
    F().foo4()
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    runner()
    assertEquals("""
            PASS
            PASS
            PASS
            PASS
            PASS
            PASS
            PASS
            PASS
            PASS
            PASS

    """.trimIndent(), sb.toString())

    return "OK"
}