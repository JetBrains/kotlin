package foo

val a = object {
    fun c() = 3
    fun b() = 2
}

fun box() : Boolean {


    if (a.c() != 3) {
        return false;
    }
    if (a.b() != 2) {
        return false;
    }


    return true;
}