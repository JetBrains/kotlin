// "Remove redundant assignment" "true"
fun foo() = 1

fun test() {
    var i: Int
    <caret>i = foo()
}