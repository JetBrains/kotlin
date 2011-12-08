namespace foo

val a1 = Array<Int>(0)

fun box() : Boolean {
    for (a in a1) {
        return false;
    }
    return true;
}