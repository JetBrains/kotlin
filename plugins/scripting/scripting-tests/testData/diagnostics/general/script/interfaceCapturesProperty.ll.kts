// LL_FIR_DIVERGENCE
// backend diagnostics are not reported in AA tests
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT


fun foo() = B().bar()

val life = 42

interface A {
    val x get() = life
}

class B : A {
    fun bar() = x
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration */
