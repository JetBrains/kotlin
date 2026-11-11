// RUN_PIPELINE_TILL: CODEGEN
// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlinx.serialization.*

fun local(): String {
    @Serializable
    data class Carrier(val i: Int)

    return Carrier.serializer().descriptor.toString()
}

fun box(): String {
    val expected = "local.Carrier(i: kotlin.Int)"
    val actual = local()
    if (expected != actual) {
        return "Fail: $actual"
    }
    return "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, ifExpression, localClass,
localProperty, primaryConstructor, propertyDeclaration, stringLiteral */
