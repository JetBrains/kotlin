package foo

import js.*

fun box(): Boolean {

    var twoToFive = 2..5

    if (twoToFive.contains(6)) return false;
    if (twoToFive.contains(1)) return false;
    if (twoToFive.contains(0)) return false;
    if (twoToFive.contains(-100)) return false;
    if (twoToFive.contains(10)) return false;
    if (!twoToFive.contains(2)) return false;
    if (!twoToFive.contains(3)) return false;
    if (!twoToFive.contains(4)) return false;
    if (!twoToFive.contains(5)) return false;
    if (!(twoToFive.start == 2)) return false;
    if (!(twoToFive.end == 5)) return false;

    var sum = 0;
    for (i in twoToFive) {
        sum += i;
    }

    if (sum != 14) return false;

    return true;

}
