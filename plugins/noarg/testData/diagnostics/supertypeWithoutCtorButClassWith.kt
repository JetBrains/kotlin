// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-80822
annotation class NoArg

@NoArg
class SomeClass : SomeBaseClass("hi")

open class SomeBaseClass(val someProperty: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
