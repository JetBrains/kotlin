public fun Test(): Test {
    return Test(null)
}

public fun Test(name: String): Test {
    return Test(Test.foo(name))
}

public class Test(private val myName: String) {
    var a: Boolean = false
    var b: Double = 0.toDouble()
    var c: Float = 0.toFloat()
    var d: Long = 0
    var e: Int = 0
    protected var f: Short = 0
    protected var g: Char = ' '

    class object {

        fun foo(n: String): String {
            return ""
        }
    }
}

public class User {
    class object {
        public fun main() {
            val t = Test("name")
        }
    }
}