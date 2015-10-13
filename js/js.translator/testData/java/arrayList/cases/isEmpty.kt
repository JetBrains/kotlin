package foo

import java.util.ArrayList;

fun box(): Boolean {
    val a = ArrayList<Int>();
    a.add(3)
    return !(a.isEmpty()) && (ArrayList<Int>().isEmpty());
}