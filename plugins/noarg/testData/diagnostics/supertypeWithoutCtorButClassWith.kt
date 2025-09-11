// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80822
annotation class NoArg

@NoArg
class SomeClass : SomeBaseClass("hi")

open class SomeBaseClass(val someProperty: String)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, stringLiteral */
