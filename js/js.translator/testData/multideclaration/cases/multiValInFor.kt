package foo

import java.util.ArrayList

class A {
    fun component1(): Int = 1
}
fun A.component2(): String = "n"

fun box(): String {
    val list = ArrayList<A>()
    list.add(A())

    var i = 0;
    var s = ""
    for ((a, b) in list) {
        i = a;
        s = b;
    }

    if (i != 1) return "i != 1, it: " + i
    if (s != "n") return "s != 'n', it: " + s

    return "OK"
}