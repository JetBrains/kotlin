// EXPECTED_REACHABLE_NODES: 495
package foo

val a1 = arrayOfNulls<Int>(10)

fun box(): String {
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
    for(i in arrayOf(0,1,2))
        try { s += "A${i}:"} finally {}
    assertEquals("A0:A1:A2:", s)

    var sLong = 0L
    var aLong = longArrayOf(1,2,3,4,5,6,7,8,9,10)
    for(i in aLong)
        sLong += i
    assertEquals(55L, sLong)

    return "OK"
}