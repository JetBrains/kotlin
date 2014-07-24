package foo

import java.util.ArrayList

fun box(): Boolean {
    var i = 0
    val list = ArrayList<Int>()
    while (i++ < 3) {
        list.add(i)
    }

    // test addAt
    list.add(0, 400)
    list.remove(0);
    list.add(1, 500)
    // test contains, addAll
    if (!list.contains(500) || list.contains(600) || list.addAll(ArrayList<Int>())) {
        return false;
    }

    val a = ArrayList<Int>()
    a.add(3)

    val b = ArrayList<Int>()
    b.add(4)
    a.addAll(b)
    if (a[0] != 3 || a[1] != 4) {
        return false
    }

    if (a.isEmpty() || !ArrayList<Int>().isEmpty()) {
        return false
    }

    assertNotEquals(a, b, "a != b")

    b[0] = a[0]
    b.add(a[1])
    assertEquals(a, b, "a == b")

    a.clear()
    assertEquals(true, a.isEmpty(), "a.isEmpty()")


    assertEquals(array(1, 500, 2, 3), list.copyToArray(), "list.copyToArray()")
    assertEquals("[1,500,2,3]", JSON.stringify(list), "JSON.stringify(list)")
    assertEquals("[1, 500, 2, 3]", list.toString(), "list.toString()")
    return true;
}