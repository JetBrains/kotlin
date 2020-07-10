class C {
    fun <caret>f(p1: Int, p2: Int) {
        println(p1)
        println(p2)
    }

    fun g(other: C) {
        other?.f(5, 6)
        C()?.f(2, 3)
    }
}
