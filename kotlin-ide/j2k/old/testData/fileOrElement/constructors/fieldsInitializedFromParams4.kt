internal class C(p: Int, c: C) {
    var p: Int = 0

    init {
        c.p = p
    }
}