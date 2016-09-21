// MODULE: lib1
// FILE: lib1.kt
package lib1

interface A {
    private fun foo() = "A.foo"

    fun bar() = foo()
}

// MODULE: lib2
// FILE: lib2.kt
package lib2

interface B {
    private fun foo() = "B.foo"

    fun bar() = foo()
}

// MODULE: main(lib1,lib2)
// FILE: main.kt
package main

import lib1.A
import lib2.B

class Derived1 : A, B {
    override fun bar() = super<A>.bar()
}

class Derived2 : A, B {
    override fun bar() = super<B>.bar()
}

// NOTE. This test is fragile, it may fail due to unexpected (and correct) changes in algorithm that assigns
// unique identifiers to non-public declarations. However, we don't see any way of doing such test so that
// it won't report false positives eventually. So be patient and just update this test whenever you changed
// algorithm of assigning unique identifiers.
// Please, check that A.foo and B.foo have different JS names.
private fun checkJsNames(o: dynamic): Boolean = "foo_2pru9n\$_0" in o && "foo_2psha1\$_0" in o

fun box(): String {
    val a = Derived1()
    if (a.bar() != "A.foo") return "fail1: ${a.bar()}"

    val b = Derived2()
    if (b.bar() != "B.foo") return "fail2: ${b.bar()}"

    if (!checkJsNames(a)) return "fail3"
    if (!checkJsNames(b)) return "fail4"

    return "OK"
}