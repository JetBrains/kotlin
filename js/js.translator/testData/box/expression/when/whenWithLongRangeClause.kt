// EXPECTED_REACHABLE_NODES: 493
// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange in general case of a class that has rangeTo method
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10) + testFun(150) + testFun2(50) + testFun2(5)
    return if (result == "misshithithitmisshit![miss][hit]") "OK" else "fail"
}
fun testFun(index: Long): String {
    return when (index) {
        in 0..9 -> "hit"
        in 100L..200L -> "hit!"
        else -> "miss"
    }
}
fun testFun2(index: Int): String {
    return when (index) {
        in 0L..9L -> "[hit]"
        else -> "[miss]"
    }
}