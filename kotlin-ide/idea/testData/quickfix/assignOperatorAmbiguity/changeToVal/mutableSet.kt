// "Change 'set' to val" "true"
// WITH_RUNTIME

fun test() {
    var set = mutableSetOf(1)
    set <caret>+= 2
}