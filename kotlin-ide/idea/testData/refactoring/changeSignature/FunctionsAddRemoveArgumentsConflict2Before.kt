protected fun foo(x1: Int = 1, x2: Float, x3: ((Int) -> Int)?) {
    foo(<caret>2, 3.5, null);
    foo(x3 = null, x2 = 5.5, x1 = 4);
}

fun bar() {
    foo(x1 = 2, x2 = 3.5, x3 = null);
    foo(x3 = null, x1 = 3, x2 = 4.5);
    foo(x3 = null, x2 = 5.5, x1 = 4);
}
