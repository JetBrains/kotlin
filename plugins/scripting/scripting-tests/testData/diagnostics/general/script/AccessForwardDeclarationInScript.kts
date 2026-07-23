// RUN_PIPELINE_TILL: FRONTEND

val x = <!UNINITIALIZED_VARIABLE!>y<!>

val y = 2

/* GENERATED_FIR_TAGS: integerLiteral, localProperty, propertyDeclaration */
