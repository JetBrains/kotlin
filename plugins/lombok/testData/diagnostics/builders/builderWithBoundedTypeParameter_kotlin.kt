// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder

@Builder
class BoundedTypeParameter<T : CharSequence>(val value: T)

fun test() {
    BoundedTypeParameter.builder<<!UPPER_BOUND_VIOLATED!>Int<!>>().value(1).build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration,
typeConstraint, typeParameter */
