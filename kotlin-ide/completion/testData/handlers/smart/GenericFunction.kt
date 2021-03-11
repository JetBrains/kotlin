class C<T>

fun <T> foo(p1: C<T>, p2: T){}

fun bar(s: String) {
    foo(C<String>(), <caret>)
}

//ELEMENT: s