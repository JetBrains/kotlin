// ERROR: Property must be initialized or be abstract
class Test {
    private val s: String?
    internal var b = false
    internal var d = 0.0

    constructor() {
        b = true
    }

    constructor(s: String?) {
        this.s = s
    }
}
