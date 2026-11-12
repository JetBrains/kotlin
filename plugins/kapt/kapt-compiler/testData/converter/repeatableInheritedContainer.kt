// FULL_JDK

@Repeatable
@java.lang.annotation.Inherited
annotation class A(val value: String)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@java.lang.annotation.Inherited
@Repeatable
@Retention(AnnotationRetention.BINARY)
annotation class B(val value: String)

@A("a1")
@A("a2")
@B("b1")
@B("b2")
open class Base
