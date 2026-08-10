// LL_FIR_DIVERGENCE
// backend diagnostics are not reported in AA tests
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// KT-19423 variation

val used = "abc"

class Outer {
    class User {
        val property = used
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, propertyDeclaration, stringLiteral */
