// WITH_RUNTIME
// IS_APPLICABLE: false
fun foo(list: List<String>): Int? {
    var index = 0
    <caret>for ((i, s) in list.withIndex()) {
        val x = s.length * index * i
        index++
        if (x > 0) return x
    }
    return null
}