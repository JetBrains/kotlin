/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// Private classes
private open class A {
    public open fun foo1() = println("FAIL")
    internal open fun foo2() = println("FAIL")
    protected open fun foo3() = println("FAIL")
    private fun foo4() = println("FAIL")
}

private class B:A() {
    override public fun foo1() = println("PASS")
    override internal fun foo2() = println("PASS")
    override protected fun foo3() = println("PASS")
    private fun foo4() = println("PASS")
    fun foo5() = foo3()
    fun foo6() = foo4()
}

private abstract class G {
    public abstract fun foo1()
    internal abstract fun foo2()
    protected abstract fun foo3()
    private fun foo4() = println("FAIL")
}

private class H:A() {
    override public fun foo1() = println("PASS")
    override internal fun foo2() = println("PASS")
    override protected fun foo3() = println("PASS")
    private fun foo4() = println("PASS")
    fun foo5() = foo3()
    fun foo6() = foo4()
}


// Private interfaces
private interface C {
    fun foo() = println("FAIL")
}

private class D: C {
    override fun foo() = println("PASS")
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
        override public fun foo1() = println("PASS")
        override internal fun foo2() = println("PASS")
        override protected fun foo3() = println("PASS")
        private fun foo4() = println("PASS")
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
       public open fun foo1() = println("FAIL")
       internal open fun foo2() = println("FAIL")
       protected open fun foo3() = println("FAIL")
       private fun foo4() = println("FAIL")
   }
   class F : E() {
       public override fun foo1() = println("PASS")
       internal override fun foo2() = println("PASS")
       protected override fun foo3() = println("PASS")
       private fun foo4() = println("PASS")
       fun foo5() = foo3()
       fun foo6() = foo4()
   }
   F().foo1()
   F().foo2()
   F().foo5()
   F().foo6()
}

