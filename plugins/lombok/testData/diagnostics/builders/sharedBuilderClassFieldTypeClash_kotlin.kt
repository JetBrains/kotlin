// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89048
// FIR_DUMP
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

import lombok.Builder

// Both constructor builders populate the same `ClashBuilder` class, which can hold only one field `x`.
// Only `x: String` is generated, yet both `x(String)` and `x(Int)` setters are, the latter storing an `Int` into a `String` field.
// Should be a conflict reported (Lombok skips the second `x` setter and `build()`, and javac then rejects the second builder).
class <!LOMBOK_GENERATED_DECLARATION_CLASH!>Clash<!> {
    @Builder(builderMethodName = "first")
    constructor(x: String)

    @Builder(builderMethodName = "second")
    constructor(x: Int)
}

/* GENERATED_FIR_TAGS: classDeclaration, secondaryConstructor, stringLiteral */
