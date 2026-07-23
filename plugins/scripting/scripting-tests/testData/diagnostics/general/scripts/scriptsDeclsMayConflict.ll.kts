// LL_FIR_DIVERGENCE
// KT-62861
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// FILE: script1.kts

val a = 42

// FILE: script2.kts

val a = "42"

// FILE: main.kt

fun foo() =  <!UNRESOLVED_REFERENCE!>a<!>

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration, stringLiteral */
