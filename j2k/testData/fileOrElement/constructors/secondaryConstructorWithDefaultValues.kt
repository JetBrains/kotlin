// ERROR: Val cannot be reassigned
// ERROR: Val cannot be reassigned
internal class A {
    private val s = ""
    private val x = 0

    constructor() {}

    @JvmOverloads
    constructor(p: Int, s: String, x: Int = 1) {
        this.s = s
        this.x = x
    }
}