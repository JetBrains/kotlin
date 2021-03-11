// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(bar: Int?) {
    bar +<caret> 1
}