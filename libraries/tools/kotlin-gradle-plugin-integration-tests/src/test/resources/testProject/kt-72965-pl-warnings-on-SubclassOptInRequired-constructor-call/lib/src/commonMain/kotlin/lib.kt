annotation class A

@OptIn(kotlin.ExperimentalSubclassOptIn::class)
fun foo(): Any = SubclassOptInRequired(A::class)
