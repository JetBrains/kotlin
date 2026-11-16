// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP

annotation class AllOpen

@AllOpen
data class Some(val x: Int)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, primaryConstructor, propertyDeclaration */
