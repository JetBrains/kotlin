package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10)
    return if (result == "misshithithitmiss" && SideEffect.invocationCount == 5) "OK" else "fail"
}
fun testFun(index: Int): String {
    val se = SideEffect(index)
    return when (se.get()) {
        in 0..9 -> "hit"
        else -> "miss"
    }
}
class SideEffect(var value: Int) {
    fun get(): Int {
        invocationCount++
        return value
    }

    companion object {
        var invocationCount = 0
    }
}
// see KT-7683