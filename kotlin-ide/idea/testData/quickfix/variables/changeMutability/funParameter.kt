// "Change to var" "false"
// ACTION: Remove redundant assignment
// ERROR: Val cannot be reassigned
fun fun1(i: Int) {
    <caret>i = 2
}