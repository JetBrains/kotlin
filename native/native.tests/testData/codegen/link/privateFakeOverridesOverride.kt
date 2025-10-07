// MODULE: lib
// FILE: lib.kt

val sb = StringBuilder()

// Private classes
private open class A {
    public open fun foo1() = sb.appendLine("FAIL")
    internal open fun foo2() = sb.appendLine("FAIL")
    protected open fun foo3() = sb.appendLine("FAIL")
    private fun foo4() = sb.appendLine("FAIL")
}

private class B:A() {
    override public fun foo1() = sb.appendLine("PASS")
    override internal fun foo2() = sb.appendLine("PASS")
    override protected fun foo3() = sb.appendLine("PASS")
    private fun foo4() = sb.appendLine("PASS")
    fun foo5() = foo3()
    fun foo6() = foo4()
}

private abstract class G {
    public abstract fun foo1()
    internal abstract fun foo2()
    protected abstract fun foo3()
    private fun foo4() = sb.appendLine("FAIL")
}

private class H:A() {
    override public fun foo1() = sb.appendLine("PASS")
    override internal fun foo2() = sb.appendLine("PASS")
    override protected fun foo3() = sb.appendLine("PASS")
    private fun foo4() = sb.appendLine("PASS")
    fun foo5() = foo3()
    fun foo6() = foo4()
}


// Private interfaces
private interface C {
    fun foo() = sb.appendLine("FAIL")
}

private class D: C {
    override fun foo() = sb.appendLine("PASS")
}

fun runner() {
    B().foo1()
    B().foo2()
    B().foo5()
    B().foo6()

    H().foo1()
    H().foo2()
    H().foo5()
    H().foo6()

    D().foo()

    // Objects
    object : A(){
        override public fun foo1() = sb.appendLine("PASS")
        override internal fun foo2() = sb.appendLine("PASS")
        override protected fun foo3() = sb.appendLine("PASS")
        private fun foo4() = sb.appendLine("PASS")
        fun foo5() = foo3()
        fun foo6() = foo4()
    }.apply {
        foo1()
        foo2()
        foo5()
        foo6()
    }

    // Function local classes
    open class E {
        public open fun foo1() = sb.appendLine("FAIL")
        internal open fun foo2() = sb.appendLine("FAIL")
        protected open fun foo3() = sb.appendLine("FAIL")
        private fun foo4() = sb.appendLine("FAIL")
    }
    class F : E() {
        public override fun foo1() = sb.appendLine("PASS")
        internal override fun foo2() = sb.appendLine("PASS")
        protected override fun foo3() = sb.appendLine("PASS")
        private fun foo4() = sb.appendLine("PASS")
        fun foo5() = foo3()
        fun foo6() = foo4()
    }
    F().foo1()
    F().foo2()
    F().foo5()
    F().foo6()
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