class Foo {
    /* Number of parameters increases, relevant for ordering */
    fun a() = Unit
    fun a(p0: Int) = Unit
    fun a(p0: Int, p1: Int) = Unit

    /* Should be ordered a, b, c, but is explicitly placed as a, c, b in source code */
    fun c() = Unit

    /* Number of parameters decreases, should be orderd in increasing number */
    fun b(p0: Int, p1: Int) = Unit
    fun b(p0: Int) = Unit
    fun b() = Unit

    /* Short get sorted in alphabetical order */
    val pA = 42
    val pC = 42
    val pB = 42

    /* Functions should be prioritised over properties */
    fun d() = 42
}
