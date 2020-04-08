// "Remove redundant 'if' statement" "false"
// ACTION: Add braces to 'if' statement
// ACTION: Invert 'if' condition
// ACTION: Replace 'if' with 'when'

fun bar(p: Int) {
    var v1 = false
    var v2 = false
    <caret>if (p > 0) v2 = true else v1 = false
}