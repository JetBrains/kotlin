package foo

fun countKeysVar(a: Array<Int>): Int = js("""
    let result = 0;
    for (var key in a) {
        result += 1;
    }
    return result;
""")

fun countKeysConst(a: Array<Int>): Int = js("""
    let result = 0;
    for (const key in a) {
        result += 1;
    }
    return result;
""")

fun countKeysLet(a: Array<Int>): Int = js("""
    let result = 0;
    for (let key in a) {
        result += 1;
    }
    return result;
""")

fun countKeysExisting(a: Array<Int>): Int = js("""
    let result = 0;
    let key = null;
    for (key in a) {
        result += 1;
    }
    return result;
""")

fun countKeysInExpression(a: Array<Int>): Int = js("""
    let result = 0;
    for (const key in (function () { return a })()) {
        result += 1;
    }
    return result;
""")

fun box(): String {
    assertEquals(3, countKeysVar(arrayOf(1,2,3)))
    assertEquals(3, countKeysConst(arrayOf(1,2,3)))
    assertEquals(3, countKeysLet(arrayOf(1,2,3)))
    assertEquals(3, countKeysExisting(arrayOf(1,2,3)))
    assertEquals(3, countKeysInExpression(arrayOf(1,2,3)))

    return "OK"
}