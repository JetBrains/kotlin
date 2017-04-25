// EXPECTED_REACHABLE_NODES: 495
package foo

interface AL {
    fun get(index: Int): Any? = null
}

class SmartArrayList() : AL {
}

fun box(): String {
    val c = SmartArrayList()
    return if (null == c.get(0)) return "OK" else "fail"
}