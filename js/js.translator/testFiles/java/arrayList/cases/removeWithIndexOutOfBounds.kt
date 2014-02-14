package foo

import java.util.ArrayList

fun box(): Boolean {
    var threwForEmptyList = false

    val arr = ArrayList<Int>()
    try {
        arr.remove(2)
    }
    catch(e: IndexOutOfBoundsException) {
        threwForEmptyList = true
    }

    for (i in 0..10) {
        arr.add(i)
    }

    var threwForFilled = false

    try {
        arr.remove(20)
    }
    catch(e: IndexOutOfBoundsException) {
        threwForFilled = true
    }

    return threwForEmptyList && threwForFilled
}