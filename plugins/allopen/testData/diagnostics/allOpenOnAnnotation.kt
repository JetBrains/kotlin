// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-63507

annotation class AllOpen

@AllOpen
annotation class SubComponent(
    val scope: String
)

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration */
