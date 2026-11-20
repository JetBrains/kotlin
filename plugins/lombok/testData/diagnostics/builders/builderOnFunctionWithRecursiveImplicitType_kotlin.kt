// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder

// The implicit return type of `create` depends on the return type of `build()`, which is the return type of `create`.
class Recursive(val x: Int) {
    companion object {
        <!BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE!>@Builder(builderClassName = "RecursiveBuilder")<!>
        fun create(x: Int) = if (x > 0) builder().x(x - 1).<!UNRESOLVED_REFERENCE!>build<!>() else Recursive(x)
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, comparisonExpression, functionDeclaration,
ifExpression, integerLiteral, objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
