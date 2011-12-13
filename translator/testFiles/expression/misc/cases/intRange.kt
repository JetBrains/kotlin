namespace foo

class RangeIterator(val start : Int, var count : Int, val reversed : Boolean) {

    fun next() : Int {
        --count
        return start + if (reversed) -count else count;
    }


    fun hasNext() = (count > 0);
}

class NumberRange(val start : Int, val size : Int, val reversed : Boolean) {

    val end : Int
    get() = start + size

    fun contains(number : Int) : Boolean {
        if (reversed) {
            return (number <= start) && (number > start - size);
        } else {
            return (number >= start) && (number < start + size);
        }
    }

    fun iterator() = RangeIterator(start, size, reversed);
}



fun box() = testRange() && testReversedRange();

fun testRange() : Boolean {

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
    if (!(oneToFive.end == 5)) return false;

    var sum = 0;
    for (i in oneToFive) {
        sum += i;
    }
    for (i in oneToFive) {
        System.out?.print(i)
    }

    if (sum != 10) return false;

    return true;

}

fun testReversedRange() : Boolean {

    System.out?.println("Testing revesed range.");

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

    for (i in tenToFive) {
        System.out?.println(i)
    }


    var sum = 0;
    for (i in tenToFive) {
        sum += i;
    }

    if (sum != 40) {
        return false;
    }

    return true;
}

fun main(args : Array<String>) {
    System.out?.println(box())
}