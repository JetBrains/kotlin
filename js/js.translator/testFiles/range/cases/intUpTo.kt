package foo

import java.util.ArrayList

fun box(): Boolean {
    var elems = ArrayList<Int>()
    for (i in 0 rangeTo 5) {
        elems.add(i)
    }
    return elems[0] == 0 && elems[1] == 1 && elems[2] == 2 && elems[3] == 3 && elems[4] == 4 && elems[5] == 5
}