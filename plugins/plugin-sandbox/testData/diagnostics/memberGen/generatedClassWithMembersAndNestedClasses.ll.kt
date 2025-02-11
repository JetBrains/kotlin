// LL_FIR_DIVERGENCE
// KT-75132
// LL_FIR_DIVERGENCE
package bar

import foo.<!UNRESOLVED_IMPORT!>AllOpenGenerated<!>
import org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested

@ExternalClassWithNested
class Foo {
    fun foo() {}
}

@ExternalClassWithNested
class Bar {
    fun bar() {}
}

fun testConstructor() {
    val generatedClass: <!UNRESOLVED_REFERENCE!>AllOpenGenerated<!> = <!UNRESOLVED_REFERENCE!>AllOpenGenerated<!>()
}

fun testNestedClasses() {
    val nestedFoo = <!UNRESOLVED_REFERENCE!>AllOpenGenerated<!>.NestedFoo()
    nestedFoo.materialize().foo()

    val nestedBar = <!UNRESOLVED_REFERENCE!>AllOpenGenerated<!>.NestedBar()
    nestedBar.materialize().bar()
}

