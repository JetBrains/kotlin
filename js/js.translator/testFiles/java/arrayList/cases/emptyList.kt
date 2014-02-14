package foo

import java.util.ArrayList;

fun box(): Boolean {
    val a = ArrayList<Int>();
    return a.size() == 0;
}