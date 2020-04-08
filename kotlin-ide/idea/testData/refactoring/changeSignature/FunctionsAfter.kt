fun foo(_x1: Int = 1, _x2: Float?, _x3: ((Int) -> Int)?) {
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
