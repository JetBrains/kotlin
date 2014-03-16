package foo

import java.util.*

fun box(): Boolean {
    val data = ArrayList<String>()
    data.add("foo")
    data.add("bar")
    data.add("whatnot")
    val data2 = ArrayList<String>()
    data2.addAll(data)
    return data.equals(data2)
}
