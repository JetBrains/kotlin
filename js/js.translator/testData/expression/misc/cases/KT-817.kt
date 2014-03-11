package foo

class Range() {

    val reversed = false;
    val start = 0;
    var count = 10;

    fun next() = start + if (reversed) -(--count) else (--count);
}

fun box(): Boolean {
    val r = Range()
    if (r.next() != 9) {
        return false;
    }
    if (r.next() != 8) {
        return false;
    }
    return true;
}