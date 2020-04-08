// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableCollection<Int>) {
    <caret>for (s in list) {
        for (i in s.indices) {
            if (i > 1000) break
            target.add(i)
        }
    }
}