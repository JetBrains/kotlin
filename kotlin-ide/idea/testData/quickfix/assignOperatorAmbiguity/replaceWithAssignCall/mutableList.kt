// "Replace with 'plusAssign()' call" "true"
// WITH_RUNTIME

fun test() {
    var list = mutableListOf(1)
    list <caret>+= 2
}