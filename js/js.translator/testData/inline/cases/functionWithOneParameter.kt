package foo

fun box(): Boolean {
    val t = myInlineFun(1 == 2)
    val f = myInlineFun(1 == 2)
    return t and f
}

inline fun myInlineFun(t: Boolean) = !t
