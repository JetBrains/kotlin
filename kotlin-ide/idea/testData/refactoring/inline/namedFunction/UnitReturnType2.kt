fun <caret>f(p1: Int, p2: Int) {
    println(p1)
    nonUnit(p2)
}

fun nonUnit(p: Int): Int = p

fun <T> doIt(p: () -> T): T = TODO()

fun g(p: String?) {
    f(1, 2)

    p?.let { f(3, 4) }
}

fun h() = f(5, 6)

fun x() = doIt { f(7, 8) }
