//ALLOW_AST_ACCESS
package test

@Target(AnnotationTarget.TYPE)
annotation class A

fun @A String.foo() {}
