package foo

fun box(): String {
    var n = 0
    var r = 3..0
    for(i in r)
        n++
    assertEquals(0, n)

    r = 0..3
    for(i in r)
        n++
    assertEquals(4, n)

    var sLong = 0L
    var rLong = 0L..10L
    for(i in rLong)
        sLong += i
    assertEquals(55L, sLong)

    var sDouble = 0.0
    var rDouble = 0L..10.0
    for(i in rDouble)
        sDouble += i
    assertEquals(55.0, sDouble)

    return "OK"
}