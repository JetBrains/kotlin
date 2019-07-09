inline fun foo(x: Int): String {
    if (x == 1) {
        return "a" +
               x
    }

    print("!")

    if (x == 2) {
        return "b" +
               x
    }

    println("#")

    return x.toString()
}

fun bar(x: Int) {
    println(foo(x + 1))
    println("%")
}

// LINES: 1 1 1 1 1 1 1 1 1 17 2 2 3 3 4 7 7 9 9 10 10 11 14 14 16 16 22 20 20 20 20 2 2 3 3 4 3 7 7 9 9 10 10 11 10 14 14 16 16 * 20 21 21