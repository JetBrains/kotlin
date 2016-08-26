package foo

var x = 1

val y = 2

fun box(): String {
    var refX = ::x
    assertEquals(1, refX.get())
    assertEquals("x", refX.name)

    refX.set(100)
    assertEquals(100, x)

    var refY = ::y
    assertEquals(2, refY.get())
    assertEquals("y", refY.name)

    return "OK"
}
