// KJS_WITH_FULL_RUNTIME

fun getC(c: Char) = c
inline fun getCInline(c: Char) = c

// CHECK_NEW_COUNT: function=testNonInlineEquals1 count=0
fun testNonInlineEquals1(a: Char, b: Char) : Boolean {
    return getC(a) == getC(b)
}

// CHECK_NEW_COUNT: function=testNonInlineEquals2 count=0
fun testNonInlineEquals2(a: Char, b: Char) : Boolean {
    val a1 = getC(a)
    val b1 = getC(b)
    return a1 == b1
}

// CHECK_NEW_COUNT: function=testInlineEquals1 count=0
fun testInlineEquals1(a: Char, b: Char) : Boolean {
    return getCInline(a) == getCInline(b)
}

// CHECK_NEW_COUNT: function=testInlineEquals2 count=0
fun testInlineEquals2(a: Char, b: Char) : Boolean {
    val a1 = getCInline(a)
    val b1 = getCInline(b)
    return a1 == b1
}

// CHECK_NEW_COUNT: function=testStringAppend1 count=0
fun testStringAppend1(s1: String, b: Char, s2: String) : Boolean {
    val s = s1 + b
    return s == s2
}

// CHECK_NEW_COUNT: function=testStringAppend2 count=0
fun testStringAppend2(a: Char, s1: String, s2: String) : Boolean {
    val s = a + s1
    return s == s2
}

// CHECK_NEW_COUNT: function=testStringAppendInline1 count=0
fun testStringAppendInline1(s1: String, b: Char, s2: String) : Boolean {
    val s = s1 + getCInline(b)
    return s == s2
}

// CHECK_NEW_COUNT: function=testStringAppendInline2 count=0
fun testStringAppendInline2(a: Char, s1: String, s2: String) : Boolean {
    val s = getCInline(a) + s1
    return s == s2
}

// CHECK_NEW_COUNT: function=testStringBuild count=0
fun testStringBuild(s1: String, b: Char, s2: String) : Boolean {
    val s = "$s1 $b "
    return s == s2
}

// CHECK_NEW_COUNT: function=testStringBuildInline count=0
fun testStringBuildInline(s1: String, b: Char, s2: String) : Boolean {
    val s = "$s1 ${getCInline(b)} "
    return s == s2
}

fun box(): String {
    if (!testNonInlineEquals1('a', 'a')) {
        return "Fail testNonInlineEquals1"
    }
    if (!testNonInlineEquals2('b', 'b')) {
        return "Fail testNonInlineEquals2"
    }
    if (!testInlineEquals1('c', 'c')) {
        return "Fail testInlineEquals1"
    }
    if (!testInlineEquals2('d', 'd')) {
        return "Fail testInlineEquals2"
    }
    if (!testStringAppend1("hello", 'x', "hellox")) {
        return "Fail testStringAppend1"
    }
    if (!testStringAppend2('x', "hello", "xhello")) {
        return "Fail testStringAppend2"
    }
    if (!testStringAppendInline1("hello", 'x', "hellox")) {
        return "Fail testStringAppendInline1"
    }
    if (!testStringAppendInline2('x', "hello", "xhello")) {
        return "Fail testStringAppendInline2"
    }
    if (!testStringBuild("hello", 'x', "hello x ")) {
        return "Fail testStringBuild"
    }
    if (!testStringBuildInline("hello", 'x', "hello x ")) {
        return "Fail testStringBuildInline"
    }
    return "OK"
}
