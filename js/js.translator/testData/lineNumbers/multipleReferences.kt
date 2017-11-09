fun box(x: Int) {
    println(x)
    println("split")
    println(x)

    println(O.y)
    println("split")
    println(O.y)
}

object O {
    val y = 23
}

// LINES: 9 2 2 3 3 4 4 6 6 7 7 8 8 11 11 12 12 * 11 11 11 11 11 11
