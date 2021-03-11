// IS_APPLICABLE: true

fun bar() {
    val l: Foo<Int> = foo<caret><Int>()
}

class Foo<T>

fun <T> foo(): Foo<T> = Foo()
