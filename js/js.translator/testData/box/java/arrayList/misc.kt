// EXPECTED_REACHABLE_NODES: 896
package foo


fun box(): String {
    var i = 0
    val list = ArrayList<Int>()
    while (i++ < 3) {
        list.add(i)
    }

    // test addAt
    list.add(0, 400)
    list.removeAt(0);
    list.add(1, 500)
    // test contains, addAll
    if (!list.contains(500) || list.contains(600) || list.addAll(ArrayList<Int>())) {
        return "fail1"
    }

    val a = ArrayList<Int>()
    a.add(3)

    val b = ArrayList<Int>()
    b.add(4)
    a.addAll(b)
    if (a[0] != 3 || a[1] != 4) {
        return "fail2"
    }

    if (a.isEmpty() || !ArrayList<Int>().isEmpty()) {
        return "fail2"
    }

    assertNotEquals(a, b, "a != b")

    b[0] = a[0]
    b.add(a[1])
    assertEquals(a, b, "a == b")

    a.clear()
    assertEquals(true, a.isEmpty(), "a.isEmpty()")


    assertArrayEquals(arrayOf(1, 500, 2, 3), list.toTypedArray(), "list.toTypedArray()")
    assertEquals("[1,500,2,3]", JSON.stringify(list), "JSON.stringify(list)")
    assertEquals("[1, 500, 2, 3]", list.toString(), "list.toString()")

    return "OK"
}