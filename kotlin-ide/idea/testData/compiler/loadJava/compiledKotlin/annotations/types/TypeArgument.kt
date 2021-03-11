// ALLOW_AST_ACCESS

package test

@Target(AnnotationTarget.TYPE)
annotation class A

fun foo(bar: Map<@A String, List<@A Int>>) {}
