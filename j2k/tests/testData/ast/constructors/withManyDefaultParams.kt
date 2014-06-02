public open class Test private(private val myName: String?, private var a: Boolean, private var b: Double, private var c: Float, private var d: Long, private var e: Int, private var f: Short, private var g: Char) {
    class object {

        public open fun init(): Test {
            val __ = Test(null, false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
            return __
        }

        public open fun init(name: String?): Test {
            val __ = Test(foo(name), false, 0.toDouble(), 0.toFloat(), 0, 0, 0, ' ')
            return __
        }


        open fun foo(n: String?): String? {
            return ""
        }
    }
}

public open class User() {
    class object {
        public open fun main() {
            var t: Test? = Test.init("name")
        }
    }
}