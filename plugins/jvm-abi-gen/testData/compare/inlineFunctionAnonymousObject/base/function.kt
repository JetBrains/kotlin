package test

inline fun sum(x: Int, y: Int): Int {
    val o = object {
        val x = 42
    }
    return o.x
}