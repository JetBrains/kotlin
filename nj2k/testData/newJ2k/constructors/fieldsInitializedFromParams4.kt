internal class C(p: Int, c: C) {
    var p = 0

    init {
        c.p = p
    }
}