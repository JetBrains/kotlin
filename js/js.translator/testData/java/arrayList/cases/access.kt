package foo

import java.util.ArrayList;

fun box(): Boolean {
    val a = ArrayList<Int>();
    a.add(1)
    a.add(2)
    return (a.size() == 2) && (a.get(1) == 2) && (a.get(0) == 1);
}