// "Convert too long character literal to string" "false"
// ACTION: Introduce local variable
// ACTION: To raw string literal
// ERROR: Illegal escape: '\ '

fun foo() {
    "\ <caret>"
}