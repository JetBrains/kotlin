// FILE: main.kt
package foo

import AddSupertype1
import AddSupertype2
import AddSupertype3

import org.jetbrains.kotlin.plugin.sandbox.MetaSupertype

interface MyInterface {
    fun foo() {}
}

@AddSupertype2
class Second

@AddSupertype3
class Third

@AddSupertype1
class First

@MetaSupertype
class Zero

fun test(a: Zero, b: First, c: Second, d: Third) {
    a.<!UNRESOLVED_REFERENCE!>foo<!>() // should be an error, because meta predicate has inludeItself = false
    b.foo()
    c.foo()
    d.foo()
}

// FILE: ann3.kt
@AddSupertype2
annotation class AddSupertype3

// FILE: ann2.kt
@AddSupertype1
annotation class AddSupertype2

// FILE: ann1.kt
import org.jetbrains.kotlin.plugin.sandbox.MetaSupertype

@MetaSupertype
annotation class AddSupertype1
