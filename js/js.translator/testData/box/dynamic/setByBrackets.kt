// EXPECTED_REACHABLE_NODES: 496
package foo

fun box(): String {
    bar["num"] = "some"
    assertEquals("some", bar["num"])

    bar["str"] = "OK!"
    assertEquals("OK!", bar["str"])

    var tmp = bar["obj"]
    bar["obj"] = bar[0]
    bar[0] = tmp
    assertEquals("zero", bar["obj"])
    assertEquals(tmp, bar[0])
    assertEquals("bar.obj", bar[0].name)

    bar[5] = 27
    assertEquals(27, bar[5])

    bar[0]["name"] = "new name"
    assertEquals("new name", bar[0]["name"])

    arr["0"].it = 23
    assertEquals(23, arr["0"].it)

    arr[0][1] = "ooo"
    assertEquals("ooo", arr[0][1])

    arr[2] = 37
    assertEquals(37, arr[2])

    arr[3] = 77
    assertEquals(77, arr[3])

    arr["length"] = 3
    assertEquals(3, arr["length"])


    bar["num0"] = "new value"
    assertEquals("new value", bar["num0"])

    bar["STR"] = 12
    assertEquals(12, bar["STR"])


    bar[2] = "bar[2]"
    assertEquals("bar[2]", bar[2])

    arr["6"] = "0"
    assertEquals("0", arr["6"])
    assertEquals(7, arr.length)

    arr[10] = 15
    assertEquals(undefined, arr[7])
    assertEquals(15, arr[10])
    assertEquals(11, arr.length)

    arr["name"] = "My Array"
    assertEquals("My Array", arr["name"])

    return "OK"
}