// Some user annotation is required because annoations from kotlin.jvm are filtered out in kapt
annotation class Anno

@JvmSuppressWildcards
@Anno
class Test

@JvmSuppressWildcards(suppress = true)
class TestTrue

@JvmSuppressWildcards(suppress = false)
class TestFalse