internal class A() {
    private var s: String? = ""
    private var x = 0

    @JvmOverloads
    constructor(p: Int, s: String?, x: Int = 1) : this() {
        this.s = s
        this.x = x
    }
}