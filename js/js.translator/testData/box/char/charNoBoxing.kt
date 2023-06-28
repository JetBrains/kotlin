fun getC(c: Char) = c
inline fun getCInline(c: Char) = c

// CHECK_NEW_COUNT: function=testNonInline1 count=0
fun testNonInline1(a: Char, b: Char) : Boolean {
    return getC(a) == getC(b)
}

// CHECK_NEW_COUNT: function=testNonInline2 count=0
fun testNonInline2(a: Char, b: Char) : Boolean {
    val a1 = getC(a)
    val b1 = getC(b)
    return a1 == b1
}

// CHECK_NEW_COUNT: function=testInline1 count=0
fun testInline1(a: Char, b: Char) : Boolean {
    return getCInline(a) == getCInline(b)
}

// CHECK_NEW_COUNT: function=testInline2 count=0
fun testInline2(a: Char, b: Char) : Boolean {
    val a1 = getCInline(a)
    val b1 = getCInline(b)
    return a1 == b1
}

fun box(): String {
    if (!testNonInline1('a', 'a')) {
        return "Fail testNonInline1"
    }
    if (!testNonInline2('b', 'b')) {
        return "Fail testNonInline2"
    }
    if (!testInline1('c', 'c')) {
        return "Fail testInline1"
    }
    if (!testInline2('d', 'd')) {
        return "Fail testInline2"
    }
    return "OK"
}
