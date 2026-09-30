// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
annotation class NoArg

@NoArg
value class <!NOARG_ON_VALUE_CLASS_ERROR!>Full<!>(val x: Int, val y: Int)

@NoArg
@JvmInline
value class <!NOARG_ON_VALUE_CLASS_ERROR!>Inline<!>(val x: Int)

@NoArg
abstract value class <!NOARG_ON_VALUE_CLASS_ERROR!>Abstract<!>

abstract value class Base(x: Int)

@NoArg
value class <!NOARG_ON_VALUE_CLASS_ERROR!>Sub<!>(val x: Int, val y: Int) : Base(x)

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, primaryConstructor, propertyDeclaration, value */
