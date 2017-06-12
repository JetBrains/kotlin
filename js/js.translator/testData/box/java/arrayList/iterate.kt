// EXPECTED_REACHABLE_NODES: 886
package foo


fun box(): String {
    var i = 0
    val arr = ArrayList<Int>();
    while (i++ < 10) {
        arr.add(i);
    }
    var sum = 0
    for (a in arr) {
        sum += a;
    }
    if (sum != 55) return "fail1: $sum"
    if (arr.size != 10) return "fail2: ${arr.size}"

    return "OK"
}