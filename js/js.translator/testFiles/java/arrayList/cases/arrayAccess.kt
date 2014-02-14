package foo

import java.util.ArrayList;

fun box(): Boolean {
    val a = ArrayList<Int>();
    a.add(1)
    a.add(2)
    a[1] = 100
    return (a.size() == 2) && (a[1] == 100) && (a[0] == 1);
}