fun foo(
        a: Int =
            when (baz()) {
                1 -> bar();
                else -> 0
            }
): Int =
        a + 1

fun baz() = 1

fun bar() = 2

// LINES(JS):    1 3 3 2 2 4   3 4 4 4 5 5 2 8 8 10 10 10    12 12 12
// LINES(JS_IR): 1       2   8 * 4 3 4 5 *   8 8 10 10 10 10 12 12 12 12
