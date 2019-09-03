fun foo() {
    bar {
        try {
            baz()
        }
        catch (e: RuntimeException) {
            e.toString()
        }
    }

    bar {
        when (boo()) {
            "boo" -> baz()
            else -> "111"
        }
    }
}

inline fun bar(x: () -> String): String {
    return x()
}

fun baz() = "baz"

fun boo() = "boo"

// LINES: 1 17 9 4 6 6 7 7 3 3 3 * 13 12 13 13 14 19 21 20 20 23 23 23 25 25 25