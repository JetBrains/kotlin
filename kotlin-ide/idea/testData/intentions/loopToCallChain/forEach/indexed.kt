// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.forEachIndexed{}'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.forEachIndexed{}'"
fun foo(list: List<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        val s1 = s.substring(1)
        println(s1.hashCode() * index)
    }
}