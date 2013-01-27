package foo

import java.util.ArrayList

fun assertThat(a:Any, b:Any) {
    if (a != b) {
        throw Exception("$a is not equal to $b")
    }
}

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

    assertThat(a.equals(b), false)

    b[0] = a[0]
    b.add(a[1])
    assertThat(a.equals(b), true)

    a.clear()
    assertThat(a.isEmpty(), true)

    val array = list.toArray(Array<Int>(0, {it}))

    assertThat(array[0], 1)
    assertThat(array[1], 500)
    assertThat(array[2], 2)
    assertThat(array[3], 3)
    assertThat(JSON.stringify(list), "[1,500,2,3]")
    assertThat(list.toString(), "[1, 500, 2, 3]")
    return true;
}