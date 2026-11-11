// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

annotation class AllOpen

@AllOpen
annotation class ConsoleCommands(
    val value: String = "",
    val scope: String
)

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
