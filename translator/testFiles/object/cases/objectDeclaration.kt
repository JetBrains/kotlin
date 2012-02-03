package foo

object test {
    var c = 2;
    var b = 1;
}

fun box() : Boolean {
    if (test.c != 2) return false;

    if (test.b != 1) return false;
    test.c += 10
    if (test.c != 12) {
    return false;
    }
    return true;
}