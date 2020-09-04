@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A0
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A1
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A2
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A3
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A4
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A5
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class A6

interface P<T, K>

class X
class Y

class klass {
    fun annotatedMethod(x: @A0 P<@A1 X, P<@A2 @A3 X, @A4 Y>>, y: Array<@A5 Y>): @A6 X {
        return ""
    }
}
