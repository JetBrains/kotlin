// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters

class C

context(x: Any)
fun contextFun() {}

context(C::class) {
    contextFun()
}

context(fun() {}) {
    contextFun()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, propertyDeclaration,
starProjection */
