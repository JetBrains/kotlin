// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91706
// FILE: usage.kt
fun foo(b: Some.SomeBuilder): Some = b.build()
fun bar(b: Some.OtherBuilder): String = b.build()

// FILE: Connection.kt
import lombok.Builder

class Some private constructor(val s: String, val x: Int) {
    companion object {
        @Builder
        fun open(s: String, x: Int): Some = Some(s, x)

        @Builder(builderMethodName = "otherBuilder", builderClassName = "OtherBuilder")
        fun other(s: String, b: Boolean): String = if (b) "$s (verbose)" else s
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, ifExpression,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
