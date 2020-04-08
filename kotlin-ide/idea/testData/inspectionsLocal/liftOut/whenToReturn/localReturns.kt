// PROBLEM: none
// WITH_RUNTIME

fun test(arg: String?): Int {
    arg?.let {
        <caret>when (arg) {
            "" -> return 1
            else -> return@let 42
        }
        42
    }
    return 0
}