// "Remove redundant assignment" "false"
// ACTION: Convert assignment to assignment expression
fun foo(): Int {
    var i = 1
    <caret>i = 2
    return i
}