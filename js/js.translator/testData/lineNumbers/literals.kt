class A {
    fun box() {
        bar(
                this,
                null,
                0,
                1,
                "2",
                true,
                false
        )

        bar(
                true,
                false,
                0,
                1,
                "2",
                this,
                null
        )
    }
}

fun bar(vararg x: Any?) {
    println(x)
}

// LINES: 1 22 3 4 5 6 7 8 9 10 13 14 15 16 17 18 19 20 * 27 26 26