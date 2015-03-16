// ERROR: Property must be initialized or be abstract
public class Test {
    private val myName: String
    var a: Boolean = false
    var b: Double = 0.toDouble()
    var c: Float = 0.toFloat()
    var d: Long = 0
    var e: Int = 0
    protected var f: Short = 0
    protected var g: Char = ' '

    public constructor() {
    }

    public constructor(name: String) {
        myName = foo(name)
    }

    default object {

        fun foo(n: String): String {
            return ""
        }
    }
}

public class User {
    default object {
        public fun main() {
            val t = Test("name")
        }
    }
}