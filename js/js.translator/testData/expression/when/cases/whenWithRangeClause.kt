// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange and produce faster code when matched expression is Int
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10) + testFun(150)
    return if (result == "misshithithitmisshit!" && invocationCount == 6) "OK" else "fail"
}
fun testFun(index: Int): String {
    val se = SideEffect(index)
    return when (se.get()) {
        in 0..9 -> "hit"
        in 100.rangeTo(200) -> "hit!"
        else -> "miss"
    }
}

class SideEffect(var value: Int) {
    fun get(): Int {
        invocationCount++
        return value
    }
}
var invocationCount = 0