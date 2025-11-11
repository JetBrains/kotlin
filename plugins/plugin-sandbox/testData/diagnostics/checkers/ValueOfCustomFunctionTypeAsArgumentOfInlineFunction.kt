// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-65959

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
inline fun <T> inlineFunction(block: @MyInlineable () -> T): T {
    return block()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, nullableType, typeParameter */
