/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// Private classes
private open class A {
    public fun foo1() = println("PASS")
    internal fun foo2() = println("PASS")
    protected fun foo3() = println("PASS")
}

private class B:A() {
    fun foo4() = foo3()
}

// Private interfaces
private interface C {
    fun foo() = println("PASS")
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
       public open fun foo1() = println("PASS")
       internal open fun foo2() = println("PASS")
       protected open fun foo3() = println("PASS")
   }
   class F : E() {
       fun foo4() = foo3()
   }
   F().foo1()
   F().foo2()
   F().foo4()
}

