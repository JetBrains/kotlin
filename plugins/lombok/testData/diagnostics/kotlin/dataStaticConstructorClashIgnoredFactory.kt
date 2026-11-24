// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import lombok.*

// A vararg parameter: no factory is generated whatever the companion object declares,
// so the declared `of` clashes with nothing. Nevertheless, STATIC_CONSTRUCTOR_ALREADY_EXISTS is reported
<!STATIC_CONSTRUCTOR_ALREADY_EXISTS!>@Data(staticConstructor = "of")<!>
class DataWithVarargAndSameSignature(vararg val items: String) {
    companion object {
        fun of(vararg items: String): DataWithVarargAndSameSignature = DataWithVarargAndSameSignature(*items)
    }
}

fun test() {
    DataWithVarargAndSameSignature.of("a", "b")
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, functionDeclaration, objectDeclaration,
outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
