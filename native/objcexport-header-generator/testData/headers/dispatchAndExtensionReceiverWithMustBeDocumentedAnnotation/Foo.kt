@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
annotation class ImportantAnnotation

@MustBeDocumented
annotation class AnotherImportantAnnotation

@AnotherImportantAnnotation
class Bar {
    fun @receiver:ImportantAnnotation Any.foo() = toString()
}
