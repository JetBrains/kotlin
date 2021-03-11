// "Remove redundant 'if' statement" "true"
fun bar(value: Int): Boolean {
    val x = <caret>if (value % 2 == 0) false else true
    return x
}