// ALLOW_AST_ACCESS

package test

@Target(AnnotationTarget.TYPE)
annotation class A

class SimpleTypeAnnotation {
    fun foo(x: @A IntRange): @A Int = 42
}
