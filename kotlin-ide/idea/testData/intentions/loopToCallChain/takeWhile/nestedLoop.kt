// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= flatMap{}.takeWhile{}'"
// INTENTION_TEXT_2: "Replace with '+= asSequence().flatMap{}.takeWhile{}'"
fun foo(list: List<String>, target: MutableCollection<Int>) {
    Outer@
    <caret>for (s in list) {
        for (i in s.indices) {
            if (i > 1000) break@Outer
            target.add(i)
        }
    }
}