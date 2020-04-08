// PROBLEM: none
// WITH_RUNTIME

fun test() {
    42.<caret>also({ println(it) })
}
