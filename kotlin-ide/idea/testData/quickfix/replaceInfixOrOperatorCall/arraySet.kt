// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(array: Array<String>?) {
    array<caret>[0] = ""
}