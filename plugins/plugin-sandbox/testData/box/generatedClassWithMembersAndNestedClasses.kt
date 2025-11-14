// IGNORE_BACKEND: JS_IR, NATIVE
// IGNORE_HMPP: JS_IR
// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
//  ^Reason: KT-82482
// DUMP_IR
package bar

import foo.AllOpenGenerated
import org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested

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
