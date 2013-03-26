package foo

import java.util.ArrayList

fun box() : Boolean {
    val arr = ArrayList<Int>()
    for (i in 0..5) {
        arr.add(i)
    }

    val removed = arr.remove(2)
    return arr.size() == 5 && removed == 2 && arr[0] == 0 && arr[1] == 1 && arr[2] == 3 && arr[3] == 4 && arr[4] == 5
}