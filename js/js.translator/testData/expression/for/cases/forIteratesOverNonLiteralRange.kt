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

    return "OK"
}