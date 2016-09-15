// FQNAME: MyClass

@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Anno(val name: String)

@Anno("Mary")
@Anno("Tom")
class MyClass