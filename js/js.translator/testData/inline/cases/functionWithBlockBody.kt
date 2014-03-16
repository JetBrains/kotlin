package foo

fun box() = !myInlineFun()

inline fun myInlineFun(): Boolean {
    return false
}
