// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= takeWhile{}'"
// INTENTION_TEXT_2: "Replace with '+= asSequence().takeWhile{}'"
fun foo(list: List<String>, target: MutableCollection<String>) {
    <caret>for (s in list) {
        if (s.isEmpty()) break
        target.add(s)
    }
}