// EXPECTED_ERROR_K2: (kotlin:7:1) cannot find symbol

package test

internal annotation class Anno

@Anno
@Suppress("UNRESOLVED_REFERENCE")
internal class ClassWithParent: Foo(), Bar, Baz, CharSequence
