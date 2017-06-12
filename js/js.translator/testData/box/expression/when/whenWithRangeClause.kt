// EXPECTED_REACHABLE_NODES: 491
// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange and produce faster code when matched expression is Int
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10) + testFun(150) + testFun(800)
    if (result != "misshithithitmisshit!@@@" || invocationCount != 7) return "fail1:" + result
    result = testFun2(-1) + testFun2(0) + testFun2(9) + testFun2(10)
    if (result != "hitmissmisshit") return "fail2:" + result
    return "OK"
}
fun testFun(index: Int): String {
    val thirdRange = 500..1000
    return when (get(index)) {
        in 0..9 -> "hit"
        in 100.rangeTo(200) -> "hit!"
        in thirdRange -> "@@@"
        else -> "miss"
    }
}
fun testFun2(index: Int): String {
    return when(index) {
        !in 0..9 -> "hit"
        else -> "miss"
    }
}

fun get(value: Int): Int {
    invocationCount++
    return value
}
var invocationCount = 0