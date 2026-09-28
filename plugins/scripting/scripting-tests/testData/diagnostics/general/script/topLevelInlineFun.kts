// RUN_PIPELINE_TILL: CODEGEN

inline fun foo(f: () -> Unit) {
    f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, localProperty, propertyDeclaration */
