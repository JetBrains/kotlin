// "Add 'private' modifier" "false"
// ACTION: Convert to secondary constructor
// ACTION: Create test
// ACTION: Enable a trailing comma by default in the formatter
// ACTION: Move to class body

class My(val <caret>parameter: Int) {
    val other = parameter
}