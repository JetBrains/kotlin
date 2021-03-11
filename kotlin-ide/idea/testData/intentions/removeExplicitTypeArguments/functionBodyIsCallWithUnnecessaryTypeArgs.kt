// IS_APPLICABLE: true

fun bar(): Foo<Int> = foo<caret><Int>()

class Foo<T>

fun <T> foo(): Foo<T> = Foo()
