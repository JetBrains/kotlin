fun box() =
        foo()

fun foo() =
        23

// LINES(JS):    1 2 2 4 5 5
// LINES(JS_IR):   2 2 * 5 5
