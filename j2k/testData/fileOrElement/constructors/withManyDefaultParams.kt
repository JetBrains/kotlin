// ERROR: 'public fun Test(name: kotlin.String): Test' is already defined in root package
// ERROR: 'public constructor Test(myName: kotlin.String)' is already defined in root package
// ERROR: None of the following functions can be called with the arguments supplied:  public fun Test(): Test defined in root package public fun Test(name: kotlin.String): Test defined in root package public constructor Test(myName: kotlin.String) defined in Test
// ERROR: Overload resolution ambiguity:  public fun Test(name: kotlin.String): Test defined in root package public constructor Test(myName: kotlin.String) defined in Test
// ERROR: Overload resolution ambiguity:  public fun Test(name: kotlin.String): Test defined in root package public constructor Test(myName: kotlin.String) defined in Test
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