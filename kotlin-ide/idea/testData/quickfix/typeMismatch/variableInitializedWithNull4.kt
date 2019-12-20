// "Change type of 'x' to 'String?'" "false"
// ACTION: Remove braces from 'if' statement
// ACTION: To raw string literal
// ACTION: Convert assignment to assignment expression
// ERROR: Type mismatch: inferred type is String but Int was expected
fun foo(condition: Boolean) {
    var x = 1
    if (condition) {
        x = "abc"<caret>
    }
}