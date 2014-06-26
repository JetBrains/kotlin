public class Test private(private val myName: String) {
    var a: Boolean = false
    var b: Double = 0.toDouble()
    var c: Float = 0.toFloat()
    var d: Long = 0
    var e: Int = 0
    protected var f: Short = 0
    protected var g: Char = ' '

    class object {

        public fun create(): Test {
            return Test(null)
        }

        public fun create(name: String): Test {
            return Test(foo(name))
        }

        fun foo(n: String): String {
            return ""
        }
    }
}

public class User {
    class object {
        public fun main() {
            val t = Test.create("name")
        }
    }
}