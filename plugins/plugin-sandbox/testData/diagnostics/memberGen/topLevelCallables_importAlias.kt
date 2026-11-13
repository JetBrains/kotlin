// RUN_PIPELINE_TILL: CODEGEN
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction as Dummy

@Dummy
class MySuperClass {
    fun test() {
        val s = dummyMySuperClass(this)
        takeString(s)
    }
}

fun takeString(s: String) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, thisExpression */
