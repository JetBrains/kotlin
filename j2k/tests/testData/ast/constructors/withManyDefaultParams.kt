public class Test private(private val myName: String, var a: Boolean, var b: Double, var c: Float, var d: Long, var e: Int, protected var f: Short, protected var g: Char) {
    class object {

        public fun create(): Test {
            return Test(null, false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
        }

        public fun create(name: String): Test {
            return Test(foo(name), false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
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