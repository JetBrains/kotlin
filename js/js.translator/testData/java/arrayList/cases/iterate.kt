package foo

import java.util.ArrayList;

fun box(): Boolean {
    var i = 0
    val arr = ArrayList<Int>();
    while (i++ < 10) {
        arr.add(i);
    }
    var sum = 0
    for (a in arr) {
        sum += a;
    }
    return (sum == 55) && (arr.size() == 10)
}