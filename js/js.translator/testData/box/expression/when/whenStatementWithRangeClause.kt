// EXPECTED_REACHABLE_NODES: 488
// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange for when statement
package foo

fun box(): String {
    var result = testFun(-1) + testFun(5) + testFun(50) + testFun(150)
    return if (result == "[miss][hit1][miss][hit2]") "OK" else "fail"
}
fun testFun(index: Int): String {
    var r = "[miss]"
    when (index) {
        in 0..9 -> r = "[hit1]"
        in 100..200 -> r = "[hit2]"
    }
    return r;
}