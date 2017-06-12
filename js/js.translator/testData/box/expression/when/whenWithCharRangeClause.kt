// EXPECTED_REACHABLE_NODES: 536
// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange
package foo

fun box(): String {
    var result = testFun('1') + testFun('Q') + testFun('z')
    return if (result == "misshitmiss") "OK" else "fail"
}
fun testFun(index: Char): String {
    return when (index) {
        in 'A'..'Z' -> "hit"
        else -> "miss"
    }
}