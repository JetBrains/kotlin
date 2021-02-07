// ALLOW_AST_ACCESS

package test

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class A(val x: String, val y: Double)

class SimpleTypeParameterAnnotation {
    fun <@A("a", 1.0) T> foo(x: T) {}
}
