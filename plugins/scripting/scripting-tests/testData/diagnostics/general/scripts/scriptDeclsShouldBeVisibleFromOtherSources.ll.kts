// LL_FIR_DIVERGENCE
// KT-62861
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// FILE: script.kts

val a = 42

class A

enum class E {
    V
}

object O {
    val v = 42
}

fun foo() = 42

// FILE: main.kt

val b =  <!UNRESOLVED_REFERENCE!>a<!>

val ca = A()

val ev = E.V

val ov = O.v

val rfoo = <!UNRESOLVED_REFERENCE!>foo<!>()

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, functionDeclaration, integerLiteral, localProperty,
objectDeclaration, propertyDeclaration */
