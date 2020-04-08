// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(l: List<() -> Boolean>) {
    <caret>for (i in 0 until l.size) {
        if (l[i]()) {
            return
        }
    }
}
