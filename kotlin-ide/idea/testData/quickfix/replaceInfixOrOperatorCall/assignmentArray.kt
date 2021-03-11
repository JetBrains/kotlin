// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(array: Array<String>?) {
    var s = ""
    s = array[0]<caret>
}