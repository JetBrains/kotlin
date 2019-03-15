// EXPECTED_REACHABLE_NODES: 1289
package foo

var x = 1

val y = 2

val z: Int
    get() = 3

fun box(): String {
    var refX = ::x
    assertEquals(1, refX.get())
    assertEquals("x", refX.name)

    refX.set(100)
    assertEquals(100, x)

    var refY = ::y
    assertEquals(2, refY.get())
    assertEquals("y", refY.name)

    var refZ = ::z
    assertEquals(3, refZ.get())
    assertEquals("z", refZ.name)

    return "OK"
}
