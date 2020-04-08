// "Change 'set' to val" "true"
// WITH_RUNTIME

fun test() {
    var set = HashMap<Int, Int>()
    set <caret>+= 2 to 2
}