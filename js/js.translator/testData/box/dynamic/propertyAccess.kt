// EXPECTED_REACHABLE_NODES: 499
package foo

fun box(): String {
    assertEquals(42, bar.num)
    assertEquals("ok!", bar.str)
    assertEquals("bar.obj", bar.obj.name)
    assertEquals("baz", baz.name)

    assertEquals(undefined, bar.num0)
    assertEquals(undefined, bar.str0)
    assertEquals(undefined, bar.obj.noname)
    assertEquals(undefined, baz.boo)


    bar.num = 22
    assertEquals(22, bar.num)

    bar.str = undefined
    assertEquals(undefined, bar.str)

    bar.obj.name = "new bar.obj"
    assertEquals("new bar.obj", bar.obj.name)


    bar.num0 = 100
    assertEquals(100, bar.num0)

    bar.str0 = "new str"
    assertEquals("new str", bar.str0)

    bar.obj.noname = "noname value"
    assertEquals("noname value", bar.obj.noname)

    baz.boo = "baz"
    assertEquals("baz", baz.boo)

    return "OK"
}