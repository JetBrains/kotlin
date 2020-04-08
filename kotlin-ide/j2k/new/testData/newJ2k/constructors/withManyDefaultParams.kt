// ERROR: Property must be initialized or be abstract
class Test {
    private val myName: String
    var a = false
    var b = 0.0
    var c = 0f
    var d: Long = 0
    var e = 0
    protected var f: Short = 0
    protected var g = 0.toChar()

    constructor() {}
    constructor(name: String?) {
        myName = foo(name)
    }

    companion object {
        fun foo(n: String?): String {
            return ""
        }
    }
}

object User {
    fun main() {
        val t = Test("name")
    }
}