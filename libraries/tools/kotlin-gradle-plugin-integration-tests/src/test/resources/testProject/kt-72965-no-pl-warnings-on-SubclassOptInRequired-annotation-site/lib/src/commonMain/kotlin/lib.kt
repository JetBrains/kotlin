@RequiresOptIn
annotation class A

@OptIn(kotlin.ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(A::class)
open class Foo
