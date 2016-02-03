// see KT-7683
// WhenTranslator must recognize KtWhenConditionInRange in general case of a class that has rangeTo method
package foo

fun box(): String {
    var result = testFun(-1) + testFun(0) + testFun(5) + testFun(9) + testFun(10)
    return if (result == "misshithithitmiss") "OK" else "fail"
}
fun testFun(index: Long): String {
    return when (index) {
        in 0..9 -> "hit"
        else -> "miss"
    }
}