package foo

fun box(): String {
    val x = 0
    val y = 1
    val z = 2
    val xs = arrayListOf(z, y, x)
    xs.remove(x as Any)
    assertEquals(listOf(z, y), xs)

    return "OK"
}