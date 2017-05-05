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

// LINES: 2 3 4 6 7 8 11 12 * 11 11 11
