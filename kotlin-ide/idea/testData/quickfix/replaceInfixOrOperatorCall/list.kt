// "Replace with safe (?.) call" "true"
// WITH_RUNTIME

fun foo(list: List<String>?) {
    list<caret>[0]
}