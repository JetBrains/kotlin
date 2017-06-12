// EXPECTED_REACHABLE_NODES: 495
package foo

var global: String = ""

class A {
    var prop: Int = 0
}

fun bar(s: String, a: A): A {
    global += s
    return a
}


fun box(): String {
    val a = A()

    global = ""
    bar("A", a).prop = try { global += "B"; 10 } finally {}
    assertEquals("AB", global)
    assertEquals(10, a.prop)

    global = ""
    bar("A", a).prop += try { global += "B"; 20 } finally {}
    assertEquals("AB", global)
    assertEquals(30, a.prop)

    global = ""
    bar("A", a).prop -= try { global += "B"; 20 } finally {}
    assertEquals("AB", global)
    assertEquals(10, a.prop)

    global = ""
    bar("A", a).prop *= try { global += "B"; 2 } finally {}
    assertEquals("AB", global)
    assertEquals(20, a.prop)

    global = ""
    bar("A", a).prop /= try { global += "B"; 5 } finally {}
    assertEquals("AB", global)
    assertEquals(4, a.prop)

    global = ""
    bar("A", a).prop %= try { global += "B"; 3 } finally {}
    assertEquals("AB", global)
    assertEquals(1, a.prop)

    global = ""
    (try { bar("A", a) } finally {}).prop++
    assertEquals("A", global)
    assertEquals(2, a.prop)

    global = ""
    ++(try { bar("A", a) } finally {}).prop
    assertEquals("A", global)
    assertEquals(3, a.prop)

    return "OK"
}