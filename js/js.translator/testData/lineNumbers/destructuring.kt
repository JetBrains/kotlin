var log = ""

fun foo() {
    for (
        (
                q,
                w
                )
        in listOf(Pair("1", "2"))
    ) {
        log += q
        log += w
    }

    bar {
        (
                q,
                w
        ) ->
        log += q
        log += w
    }
}

fun bar(f: (Pair<String, String>) -> Unit) {
    f(Pair("w", "e"))
}


// LINES:             1 1 1 1 1 1 1 1 * 3 3 9 9 4 5 9 6 7 11 11 12 12 15 15 25 25 26 26 15 16 15 17 18 20 20 21 21 22 22 * 1
