// "Remove parameter 'x'" "true"
// DISABLE-ERRORS

fun f(<caret>x: Int, y: Int) {
    f(1, 2);
}

fun g(x: Int, y: Int) {
    f(x, y);
}
