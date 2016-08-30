package foo

import java.util.ArrayList

fun box(): String {
    val al = ArrayList<Int>(10)
    return if (al.size == 0) "OK" else "fail: ${al.size}"
}