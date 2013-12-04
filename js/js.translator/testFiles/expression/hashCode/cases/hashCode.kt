package foo

fun testChar(s: Char): Boolean {
    val any: Any = s
    return s.hashCode() == any.hashCode()
}

fun testString(s: String): Boolean {
    val any: Any = s
    return s.hashCode() == any.hashCode()
}

fun testInt(int: Int): Boolean {
    val any: Any = int
    return int.hashCode() == any.hashCode()
}

fun testNumber(number: Number): Boolean {
    val any: Any = number
    return number.hashCode() == any.hashCode()
}

fun box(): String {
    if (!testChar('a')) {
        return "testChar failed"
    }
    if (!testString("hello world")) {
        return "testString failed"
    }
    if (!testInt(42)) {
        return "testInt failed"
    }
    if (!testNumber(42) || !testNumber(3.14159) || !testNumber(0xdeadbeef)) {
        return "testNumber failed"
    }
    return "OK"
}