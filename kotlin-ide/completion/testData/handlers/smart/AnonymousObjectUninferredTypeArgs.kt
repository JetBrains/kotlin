interface A<T>

fun <T> foo(a: A<T>){}

fun g() {
    foo(<caret>)
}

// ELEMENT: object