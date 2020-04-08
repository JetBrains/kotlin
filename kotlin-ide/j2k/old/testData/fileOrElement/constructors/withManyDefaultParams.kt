// ERROR: Property must be initialized or be abstract
class Test {
    private val myName: String
    internal var a: Boolean = false
    internal var b: Double = 0.toDouble()
    internal var c: Float = 0.toFloat()
    internal var d: Long = 0
    internal var e: Int = 0
    protected var f: Short = 0
    protected var g: Char = ' '

    constructor() {}

    constructor(name: String) {
        myName = foo(name)
    }

    companion object {

        internal fun foo(n: String): String {
            return ""
        }
    }
}

object User {
    fun main() {
        val t = Test("name")
    }
}