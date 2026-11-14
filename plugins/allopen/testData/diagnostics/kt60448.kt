// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
annotation class AllOpen

@AllOpen
class Test {
    @JvmName("g")
    private fun f() {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, stringLiteral */
