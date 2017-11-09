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

// LINES: 8 3 3 2 2 4 3 4 4 4 5 5 2 8 8 10 10 10 12 12 12