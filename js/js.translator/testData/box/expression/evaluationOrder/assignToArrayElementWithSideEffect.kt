// EXPECTED_REACHABLE_NODES: 494
package foo

var global: String = ""

class A {
    var prop: Int = 0
}

fun bar(s: String, index: Int): Int {
    global += s
    return index
}

val baz: Int
    get() {
        global += ":baz:"; return 1
    }

fun box(): String {
    val a = arrayOf(0,1,2,3)


    global = ""
    a[bar("A", 1)] = try { global += "B"; 10 } finally {}
    assertEquals("AB", global)
    assertEquals(10, a[1])

    global = ""
    a[bar("A", 1)] += try { global += "B"; 10 } finally {}
    assertEquals("AB", global)
    assertEquals(20, a[1])

    global = ""
    a[bar("A", 1)] -= try { global += "B"; 10 } finally {}
    assertEquals("AB", global)
    assertEquals(10, a[1])

    global = ""
    a[bar("A", 1)] *= try { global += "B"; 2 } finally {}
    assertEquals("AB", global)
    assertEquals(20, a[1])

    global = ""
    a[bar("A", 1)] /= try { global += "B"; 5 } finally {}
    assertEquals("AB", global)
    assertEquals(4, a[1])

    global = ""
    a[bar("A", 1)] %= try { global += "B"; 3 } finally {}
    assertEquals("AB", global)
    assertEquals(1, a[1])

    global = ""
    a[bar("A", 1)]++
    assertEquals("A", global)
    assertEquals(2, a[1])

    global = ""
    a[try { bar("A", 1)} finally {}]++
    assertEquals("A", global)
    assertEquals(3, a[1])

    global = ""
    ++a[bar("A", 1)]
    assertEquals("A", global)
    assertEquals(4, a[1])

    global = ""
    ++a[try { bar("A", 1)} finally {}]
    assertEquals("A", global)
    assertEquals(5, a[1])

    global = ""
    a[baz] = try { global += "right"; 100 } finally {}
    assertEquals(":baz:right", global)
    assertEquals(100, a[1])

    return "OK"
}