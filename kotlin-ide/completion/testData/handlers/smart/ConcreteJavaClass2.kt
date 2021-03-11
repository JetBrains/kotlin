fun <T> foo(klass: Class<T>) {}

fun bar() {
    foo<String>(<caret>)
}

// ELEMENT: String

