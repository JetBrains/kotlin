@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn
annotation class FooAnnotation

@FooAnnotation
class FooClass

fun foo() {
    FooClass()
}