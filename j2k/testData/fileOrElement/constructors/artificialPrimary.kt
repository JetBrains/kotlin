// ERROR: 'public fun Test(s: kotlin.String): Test' is already defined in root package
// ERROR: 'public constructor Test(s: kotlin.String)' is already defined in root package
// ERROR: None of the following functions can be called with the arguments supplied:  public fun Test(): Test defined in root package public fun Test(s: kotlin.String): Test defined in root package public constructor Test(s: kotlin.String) defined in Test
// ERROR: Overload resolution ambiguity:  public fun Test(s: kotlin.String): Test defined in root package public constructor Test(s: kotlin.String) defined in Test
public fun Test(): Test {
    val __ = Test(null)
    __.b = true
    return __
}

public fun Test(s: String): Test {
    return Test(s)
}

public class Test(private val s: String) {
    var b: Boolean = false
    var d: Double = 0.toDouble()
}