package bar

import foo.AllOpenGenerated
import org.jetbrains.kotlin.fir.plugin.ExternalClassWithNested

@ExternalClassWithNested
class Foo {
    fun foo() {}
}

@ExternalClassWithNested
class Bar {
    fun bar() {}
}

fun testConstructor() {
    val generatedClass: AllOpenGenerated = AllOpenGenerated()
}

fun testNestedClasses() {
    val nestedFoo = AllOpenGenerated.NestedFoo()
    nestedFoo.materialize().foo()

    val nestedBar = AllOpenGenerated.NestedBar()
    nestedBar.materialize().bar()
}

