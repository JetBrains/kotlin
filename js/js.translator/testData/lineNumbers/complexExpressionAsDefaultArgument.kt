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

// LINES: 2 3 4 4 5 2 8 10 12