package foo

fun box(): Boolean {
    if (myInlineFun(3, 2) != 1) {
        return false;
    }
    if (myInlineFun(100, -100) != 200) {
        return false;
    }
    return true;
}

inline fun myInlineFun(l: Int, r: Int) = l - r
