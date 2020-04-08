// ERROR: Property must be initialized or be abstract
class Test {
    private val s: String
    internal var b: Boolean = false
    internal var d: Double = 0.toDouble()

    constructor() {
        b = true
    }

    constructor(s: String) {
        this.s = s
    }
}
