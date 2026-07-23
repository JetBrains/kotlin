// LL_FIR_DIVERGENCE
// backend diagnostics are not reported in AA tests
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FIR2IR
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT


fun foo() = B.bar()

val life = 42

class A {
    val x = life
}

object B {
    fun bar() = A().x
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, objectDeclaration, propertyDeclaration */
