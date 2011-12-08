namespace foo

import java.util.ArrayList;

fun box() : Boolean {
    var i = 0
    val arr = ArrayList<Int>();
    while (i++ < 10) {
        arr.add(i);
    }
    arr.remove(2)
    var sum = 0
    for (a in arr) {
        sum += a;
    }
    return ((sum == 52)  && (arr[1] == 2) && (arr[2] == 4) && (arr[3] == 5) && (arr[4] == 6) && (arr[8] == 10))
}