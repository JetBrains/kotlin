internal class C(p: Int) {
    var p: Int = 0

    init {
        this.p = 0
        if (p > 0) {
            this.p = p
        }
    }
}