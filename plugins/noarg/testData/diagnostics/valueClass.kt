// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
annotation class NoArg

@NoArg
value class Full(val x: Int, val y: Int)

@NoArg
@JvmInline
value class Inline(val x: Int)

@NoArg
abstract value class Abstract

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, value */
