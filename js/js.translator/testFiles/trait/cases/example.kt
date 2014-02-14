package foo

trait AL {
    fun get(index: Int): Any? = null
}

class SmartArrayList() : AL {
}

fun box(): Boolean {
    val c = SmartArrayList()
    return (null == c.get(0))
}