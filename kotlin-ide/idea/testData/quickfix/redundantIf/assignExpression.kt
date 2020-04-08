// "Remove redundant 'if' statement" "true"
fun bar(p: Int) {
    val v1 = <caret>if (p > 0) true else false
}