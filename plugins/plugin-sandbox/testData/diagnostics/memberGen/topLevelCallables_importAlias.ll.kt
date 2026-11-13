// LL_FIR_DIVERGENCE
// KT-91614: The standalone KotlinAnnotationsResolver doesn't resolve an annotation used through an import alias
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction as Dummy

@Dummy
class MySuperClass {
    fun test() {
        val s = <!UNRESOLVED_REFERENCE!>dummyMySuperClass<!>(this)
        takeString(s)
    }
}

fun takeString(s: String) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, thisExpression */
