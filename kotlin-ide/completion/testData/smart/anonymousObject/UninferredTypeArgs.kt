interface A<T>

fun <T> foo(a: A<T>){}

fun g() {
    foo(<caret>)
}

// EXIST: { lookupString:"object", itemText:"object : A<...>{...}" }
