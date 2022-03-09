package test

inline fun sum(x: Int, y: Int): Int {
    val o2 = object {
        val x = 42
    }
    return o2.x
}