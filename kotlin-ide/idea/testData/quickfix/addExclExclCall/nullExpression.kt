// "Add non-null asserted (!!) call" "false"
// ACTION: Add 'toString()' call
// ACTION: Change type of 'x' to 'String?'
// ACTION: Remove braces from 'if' statement
// ERROR: Type mismatch: inferred type is String? but String was expected

fun foo(arg: String?) {
    if (arg == null) {
        val x: String = arg<caret>
    }
}