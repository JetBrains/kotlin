// PROBLEM: none
// WITH_RUNTIME

fun test(i: Int?, m: MutableMap<String, Int>) {
    m.<caret>put("", 1) ?: i
}