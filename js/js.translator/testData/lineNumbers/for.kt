fun box() {
    for (x in arrayOf(1, 2, 3)) {
        println(x)
    }

    for (x in 1..10) {
        println(x)
    }

    for (x in listOf(1, 2, 3)) {
        println(x)
    }

    val xs = listOf(1, 2, 3)
    for (x in xs.indices) {
        println(x)
    }
}

// LINES(JS):    1   18 2 2 10 2 2 2 2 2 2 3 3 6 6 6 6 7 7 10 10 10 10 10 10 11 11 14 14 15 15 15 15 16 16
// LINES(JS_IR): 1 1 *  2 2 2 2 2 2 2 2 3 3 6 6 6 6 6 6 6 7 7 10 10 10 10 11 11 14 15 15 15 15 15 15 15 15 16 16

