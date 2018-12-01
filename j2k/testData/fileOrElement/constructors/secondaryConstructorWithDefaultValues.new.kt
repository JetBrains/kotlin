internal class A {
    private var s: String? = ""
    private var x = 0

    constructor() {}

    @JvmOverloads
    constructor(p: Int, s: String?, x: Int = 1) {
        this.s = s
        this.x = x
    }
}