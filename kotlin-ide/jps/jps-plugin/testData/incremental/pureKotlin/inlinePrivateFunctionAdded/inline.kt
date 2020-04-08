package foo

fun test() {
    oldInlineFun()
}

inline
private fun oldInlineFun() {
    println("oldInlineFun")
}