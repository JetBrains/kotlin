// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'flatMap{}.filter{}.mapIndexedTo(){}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().flatMap{}.filter{}.mapIndexedTo(){}'"
fun foo(list: List<String>, target: MutableCollection<Int>) {
    var i = 0
    <caret>for (s in list) {
        for (j in s.indices) {
            if (j == 10) continue
            target.add(i + j)
            i++
        }
    }
}