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

// LINES(JS_IR): 1 1  * 4 * 6 7 * 13 12 13 19 19 20 20 23 23 23 23 25 25 25 25
