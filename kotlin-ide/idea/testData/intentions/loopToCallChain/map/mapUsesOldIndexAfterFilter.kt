// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int? {
    <caret>for ((index, s) in list.withIndex()) {
        if (s.isBlank()) continue
        val x = s.length * index
        if (x > 1000) return x
    }
    return null
}