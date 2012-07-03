package foo

import java.util.ArrayList

fun box() : Boolean {
    var i = 0
    val list = ArrayList<Int>()
    while (i++ < 3) {
        list.add(i)
    }
    val array = list.toArray()
    return array[0] == 1 && array[1] == 2 && array[2] == 3
}