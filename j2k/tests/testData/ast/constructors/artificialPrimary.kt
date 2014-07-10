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