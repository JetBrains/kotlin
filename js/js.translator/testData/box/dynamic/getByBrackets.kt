// EXPECTED_REACHABLE_NODES: 498
package foo

fun box(): String {
    assertEquals(42, bar["num"])
    assertEquals("ok!", bar["str"])
    assertEquals(bar.obj, bar["obj"])
    assertEquals("zero", bar[0])
    assertEquals("e", bar[0][1])
    assertEquals(0, bar[5])
    assertEquals("1.one", bar[1].one)
    assertEquals("bar.obj", bar.obj["name"])
    assertEquals("bar.obj", bar["obj"]["name"])
    assertEquals("baz", baz["name"])
    assertEquals(0, baz["length"])
    assertEquals("is", arr["0"].it)
    assertEquals("object", arr["0"][1])
    assertEquals("is", arr[0]["it"])
    assertEquals(undefined, arr[2])
    assertEquals(-2, arr[3])
    assertEquals(5, arr["length"])

    assertEquals(undefined, bar["num0"])
    assertEquals(undefined, bar["STR"])
    assertEquals(undefined, bar[2])
    assertEquals(undefined, bar.obj["foo"])
    assertEquals(undefined, bar["obj"].foo)
    assertEquals(undefined, bar["obj"]["foo"])
    assertEquals(undefined, baz[3])
    assertEquals(undefined, baz["function"])
    assertEquals(undefined, arr["6"])
    assertEquals(undefined, arr[7])
    assertEquals(undefined, arr[10])
    assertEquals(undefined, arr["name"])

    return "OK"
}