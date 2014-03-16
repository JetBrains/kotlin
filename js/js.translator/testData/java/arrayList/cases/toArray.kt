package foo

import java.util.ArrayList

fun box(): Boolean {
    var i = 0
    val list = ArrayList<Int>()
    while (i++ < 3) {
        list.add(i)
    }

    // test addAt
    list.add(1, 500)

    val array = list.copyToArray()
    return array[0] == 1 && array[1] == 500 && array[2] == 2 && array[3] == 3 && JSON.stringify(list) == "[1,500,2,3]";
}