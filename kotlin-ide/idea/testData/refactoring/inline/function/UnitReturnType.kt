class C {
    fun <caret>f(p1: Int, p2: Int) {
        println(p1)
        println(p2)
    }

    fun <T> doIt(p: () -> T): T = TODO()

    fun g(p: String?, other: C) {
        f(1, 2)

        p?.let { f(3, 4) }

        other?.f(5, 6)
    }

    fun h() = f(7, 8)

    fun x() = doIt { f(9, 10) }
}
