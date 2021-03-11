fun foo(x: Int, cl: () -> Int): Int {
    return x + cl()
}

fun bar() {
    foo(1) {
        3
    }
}