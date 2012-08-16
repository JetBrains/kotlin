package foo


import java.util.*

fun box(): Boolean {
    val c = ArrayList<Int>()
    for (i in 0..5) {
        c.add(i)
    }

    var s = ""
    for (i in c.iterator()) {
        s = s + i.toString()
    }

    return s == "012345"
}