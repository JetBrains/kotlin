package foo

import java.util.ArrayList;

fun box(): Boolean {
    val arr = ArrayList<Int>();
    var i = 0;
    while (i++ < 10) {
        arr.add(i);
    }
    return (arr[10] == 11)
}