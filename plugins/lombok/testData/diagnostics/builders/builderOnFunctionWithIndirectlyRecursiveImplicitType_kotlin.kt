// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder

// The same as `builderOnFunctionWithRecursiveImplicitType_kotlin.kt`, with the recursion going through another function.
class IndirectlyRecursive(val x: Int) {
    companion object {
        <!BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE!>@Builder(builderClassName = "IndirectBuilder")<!>
        fun create(x: Int) = helper(x)

        fun helper(x: Int) = if (x > 0) builder().x(x - 1).<!UNRESOLVED_REFERENCE!>build<!>() else IndirectlyRecursive(x)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, comparisonExpression, functionDeclaration,
ifExpression, integerLiteral, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
