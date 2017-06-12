// EXPECTED_REACHABLE_NODES: 494
package foo

external class A {
    val xx: Int
        @JsName("getX") get

    var yy: Int
        @JsName("getY") get
        @JsName("setY") set
}

external var zz: Int
    @JsName("getZ") get
    @JsName("setZ") set

fun box(): String {
    val a = A()

    assertEquals(23, a.xx)
    assertEquals(0, a.yy)
    a.yy = 42
    assertEquals(42, a.yy)

    assertEquals(32, zz)
    zz = 232
    assertEquals(232, zz)

    return "OK"
}