// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*

@Serializable
@JvmInline
value class Foo(val i: Int)

@Serializable
class Holder(val f: Foo)

fun box(): String {
    if(!Foo.serializer().descriptor.isInline) return "Incorrect descriptor"
    val s = Json.encodeToString(Holder.serializer(), Holder(Foo(42)))
    if (s != """{"f":42}""") return s
    return "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, multilineStringLiteral, primaryConstructor, propertyDeclaration, stringLiteral, value */
