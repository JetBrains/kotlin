package foo

import java.util.ArrayList

fun box(): Boolean {
    val arr = ArrayList<Int>()
    for (i in 0..5) {
        arr.add(i)
    }

    val removedElement = arr.remove(2)
    val removed = arr.remove(4: Any)
    return arr.size() == 4 && removedElement == 2 && removed && arr[0] == 0 && arr[1] == 1 && arr[2] == 3 && arr[3] == 5
}