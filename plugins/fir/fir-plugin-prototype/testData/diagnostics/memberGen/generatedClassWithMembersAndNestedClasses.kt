package bar

import foo.AllOpenGenerated
import org.jetbrains.kotlin.fir.plugin.B

@B
class Foo {
    fun foo() {}
}

@B
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

