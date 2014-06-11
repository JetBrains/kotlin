public class Test private(private val myName: String, private var a: Boolean, private var b: Double, private var c: Float, private var d: Long, private var e: Int, private var f: Short, private var g: Char) {
    class object {

        public fun create(): Test {
            val __ = Test(0, false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
            return __
        }

        public fun create(name: String): Test {
            val __ = Test(foo(name), false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
            return __
        }


        fun foo(n: String): String {
            return ""
        }
    }
}

public class User() {
    class object {
        public fun main() {
            val t = Test.create("name")
        }
    }
}