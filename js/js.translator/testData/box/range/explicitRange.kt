// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    var twoToFive = IntRange(2, 5)

    if (twoToFive.contains(6)) return "fail1"
    if (twoToFive.contains(1)) return "fail2"
    if (twoToFive.contains(0)) return "fail3"
    if (twoToFive.contains(-100)) return "fail4"
    if (twoToFive.contains(10)) return "fail5"
    if (!twoToFive.contains(2)) return "fail6"
    if (!twoToFive.contains(3)) return "fail7"
    if (!twoToFive.contains(4)) return "fail8"
    if (!twoToFive.contains(5)) return "fail9"
    if (!(twoToFive.start == 2)) return "fail10"
    if (!(twoToFive.endInclusive == 5)) return "fail11"

    var sum = 0;
    for (i in twoToFive) {
        sum += i;
    }

    if (sum != 14) return "fail12"

    return "OK"
}
