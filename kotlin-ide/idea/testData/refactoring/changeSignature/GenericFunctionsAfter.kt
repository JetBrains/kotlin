fun <T: Any> foo(<caret>_x1: T? = null, _x2: Double?, _x3: ((T) -> T)?) {
    foo(2, 3.5, null);
    val y1 = _x1;
    val y2 = _x2;
    val y3 = _x3;
    foo(_x3 = null, _x1 = 2, _x2 = 3.5);
}

fun bar() {
    foo(_x1 = 2, _x2 = 3.5, _x3 = null);
    foo(_x3 = null, _x1 = 2, _x2 = 3.5);
}
