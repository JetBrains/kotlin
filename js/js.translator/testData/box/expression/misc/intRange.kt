// EXPECTED_REACHABLE_NODES: 500
package foo

class RangeIterator(val start: Int, var count: Int, val reversed: Boolean) {

    var i = start

    operator fun next(): Int {
        --count
        if (reversed) {
            i--
            return i + 1
        }
        else {
            i++
            return i - 1
        }
    }


    operator fun hasNext() = (count > 0);
}

class NumberRange(val start: Int, val size: Int, val reversed: Boolean) {

    val end: Int
        get() = if (reversed) start - size + 1 else start + size - 1

    fun contains(number: Int): Boolean {
        if (reversed) {
            return (number <= start) && (number > start - size);
        }
        else {
            return (number >= start) && (number < start + size);
        }
    }

    operator fun iterator() = RangeIterator(start, size, reversed);
}


fun box(): String {
    return if (testRange() && testReversedRange()) "OK" else "fail"
}

fun testRange(): Boolean {

    val oneToFive = NumberRange(1, 4, false);
    if (oneToFive.contains(5)) return false;
    if (oneToFive.contains(0)) return false;
    if (oneToFive.contains(-100)) return false;
    if (oneToFive.contains(10)) return false;
    if (!oneToFive.contains(1)) return false;
    if (!oneToFive.contains(2)) return false;
    if (!oneToFive.contains(3)) return false;
    if (!oneToFive.contains(4)) return false;
    if (!(oneToFive.start == 1)) return false;
    if (!(oneToFive.size == 4)) return false;
    if (!(oneToFive.end == 4)) return false;

    var sum = 0;
    for (i in oneToFive) {
        sum += i;
    }

    if (sum != 10) return false;

    return true;

}

fun testReversedRange(): Boolean {

    val tenToFive = NumberRange(10, 5, true);

    if (tenToFive.contains(5)) return false;
    if (tenToFive.contains(11)) return false;
    if (tenToFive.contains(-100)) return false;
    if (tenToFive.contains(1000)) return false;
    if (!tenToFive.contains(6)) return false;
    if (!tenToFive.contains(7)) return false;
    if (!tenToFive.contains(8)) return false;
    if (!tenToFive.contains(9)) return false;
    if (!tenToFive.contains(10)) return false;

    if (!(tenToFive.start == 10)) return false;
    if (!(tenToFive.size == 5)) return false;
    if (!(tenToFive.end == 6)) return false;

    var sum = 0;
    for (i in tenToFive) {
        sum += i;
    }

    if (sum != 40) {
        return false;
    }

    return true;
}
