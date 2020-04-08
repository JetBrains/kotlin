// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'takeWhile{}'"
// IS_APPLICABLE_2: false
fun foo(n: Int, list: List<Int>): List<Int>{
    val result = mutableListOf<Int>()
    <caret>for (i in list) {
        if (i < n)
            break
        else
            result.add(i)
    }
    return result
}