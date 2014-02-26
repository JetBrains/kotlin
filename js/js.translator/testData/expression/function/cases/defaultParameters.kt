package foo

fun f(a: Int = 2, b: Int = 3) = a + b

fun box(): Boolean {
    if (f(1, 2) != 3) return false;
    if (f(1, 3) != 4) return false;
    if (f(3) != 6) return false;
    if (f() != 5) return false;

    return true;
}

