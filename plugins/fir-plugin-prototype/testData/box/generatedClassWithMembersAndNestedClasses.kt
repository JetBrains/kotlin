// DUMP_IR
package bar

import foo.AllOpenGenerated
import org.jetbrains.kotlin.fir.plugin.ExternalClassWithNested

@ExternalClassWithNested
class Foo {
    fun box(): String {
        return "OK"
    }
}

fun testConstructor() {
    val generatedClass: AllOpenGenerated = AllOpenGenerated()
}

fun testNestedClasses(): String {
    val nestedFoo = AllOpenGenerated.NestedFoo()
    return nestedFoo.materialize().box()
}

fun box(): String {
    testConstructor()
    return testNestedClasses()
}
