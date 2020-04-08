fun foo(cl: () -> Int): Int {
    return x + cl()
}

fun bar() {
    foo {
        2
    }
}