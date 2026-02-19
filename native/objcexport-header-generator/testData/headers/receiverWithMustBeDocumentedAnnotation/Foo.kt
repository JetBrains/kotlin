@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class ImportantAnnotation

fun @receiver:ImportantAnnotation Any.foo() = toString()
