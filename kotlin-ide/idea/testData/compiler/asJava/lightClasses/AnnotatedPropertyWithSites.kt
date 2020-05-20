// Test
annotation class MyAnnotation
annotation class MyAnnotation2
annotation class MyAnnotation3
annotation class MyAnnotation4
annotation class MyAnnotation5
annotation class MyAnnotation6
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnnotation7

class Test(@get:MyAnnotation @set:MyAnnotation2 @setparam:MyAnnotation3 @property:MyAnnotation4 @field:MyAnnotation5 @param:MyAnnotation6 var bar: String) {
    fun @receiver:MyAnnotation7 Int.fooF() = Unit
    var @receiver:MyAnnotation7 Int.fooP
        get() = Unit
        set(value) {}
}