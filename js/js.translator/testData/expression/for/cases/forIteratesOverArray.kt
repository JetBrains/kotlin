package foo

val a1 = arrayOfNulls<Int>(10)

fun box(): Boolean {
    var c = 0
    var d = 0
    a1[3] = 3
    a1[5] = 5

    for (a: Int? in a1) {
        if (a != null) {
            c += 1;
        }
        else {
            d += 1
        }
    }
    assertEquals(2, c)
    assertEquals(8, d)

    var s: String = ""
    for(i in array(0,1,2))
        try { s += "A${i}:"} finally {}
    assertEquals("A0:A1:A2:", s)

    return true
}