// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-83377

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*

@Serializable
class Some(val t: String)

fun testGlobal() {
    val s: Some.Companion? = null
}

fun testLocal() {
    @Serializable
    class Local(val t: String)

    val s: Local.Companion? = null
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, nullableType,
primaryConstructor, propertyDeclaration */
