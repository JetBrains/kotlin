// ERROR: Property must be initialized or be abstract
public class Test {
    private val s: String
    var b: Boolean = false
    var d: Double = 0.toDouble()

    public constructor() {
        b = true
    }

    public constructor(s: String) {
        this.s = s
    }
}
