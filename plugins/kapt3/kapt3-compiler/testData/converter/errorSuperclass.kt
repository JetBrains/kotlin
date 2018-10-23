package test

internal annotation class Anno

@Anno
@Suppress("UNRESOLVED_REFERENCE")
internal class ClassWithParent: Foo(), Bar, Baz, CharSequence