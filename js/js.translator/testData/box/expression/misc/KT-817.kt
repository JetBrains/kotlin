// EXPECTED_REACHABLE_NODES: 491
package foo

class Range() {

    val reversed = false;
    val start = 0;
    var count = 10;

    fun next() = start + if (reversed) -(--count) else (--count);
}

fun box(): String {
    val r = Range()
    if (r.next() != 9) {
        return "fail1"
    }
    if (r.next() != 8) {
        return "fail2"
    }
    return "OK"
}