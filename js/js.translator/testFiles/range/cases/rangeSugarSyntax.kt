package foo

import js.*

fun box() : Boolean {

    var oneToFive = 1..5

    if (!oneToFive.contains(5)) return false;
    if (oneToFive.contains(0)) return false;
    if (oneToFive.contains(-100)) return false;
    if (oneToFive.contains(10)) return false;
    if (!oneToFive.contains(1)) return false;
    if (!oneToFive.contains(2)) return false;
    if (!oneToFive.contains(3)) return false;
    if (!oneToFive.contains(4)) return false;
    if (!(oneToFive.start == 1)) return false;
    if (!(oneToFive.size == 5)) return false;
    if (!(oneToFive.end == 5)) return false;

    var sum = 0;
    for (i in oneToFive) {
        sum += i;
    }

    if (sum != 15) return false;

    return true;

}
