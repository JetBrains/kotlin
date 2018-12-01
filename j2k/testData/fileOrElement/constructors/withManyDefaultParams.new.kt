// ERROR: Property must be initialized or be abstract
class Test {
    private val myName: String?
    internal var a = false
    internal var b = 0.0
    internal var c = 0f
    internal var d: Long = 0
    internal var e = 0
    protected var f: Short = 0
    protected var g = 0.toChar()

    constructor() {}

    constructor(name: String?) {
        myName = foo(name)
    }

    companion object {

        internal fun foo(n: String?): String {
            return ""
        }
    }
}

object User {
    fun main() {
        val t = Test("name")
    }
}