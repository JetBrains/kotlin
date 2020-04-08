fun <T: Any> foo(<caret>x1: T? = null, x2: Double, x3: ((T) -> T)?) {
    foo(2, 3.5, null);
    val y1 = x1;
    val y2 = x2;
    val y3 = x3;
    foo(x3 = null, x1 = 2, x2 = 3.5);
}

fun bar() {
    foo(x1 = 2, x2 = 3.5, x3 = null);
    foo(x3 = null, x1 = 2, x2 = 3.5);
}
