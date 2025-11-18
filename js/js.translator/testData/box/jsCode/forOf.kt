package foo

fun sumValuesVar(a: Array<Int>): Int = js("""
    let result = 0;
    for (var value of a) {
        result += value;
    }
    return result;
""")

fun sumValuesConst(a: Array<Int>): Int = js("""
    let result = 0;
    for (const value of a) {
        result += value;
    }
    return result;
""")

fun sumValuesLet(a: Array<Int>): Int = js("""
    let result = 0;
    for (let value of a) {
        result += value;
    }
    return result;
""")

fun sumValuesExisting(a: Array<Int>): Int = js("""
    let result = 0;
    let value = null;
    for (value of a) {
        result += value;
    }
    return result;
""")

fun sumValuesInExpression(a: Array<Int>): Int = js("""
    let result = 0;
    for (const value of (function () { return a })()) {
        result += value;
    }
    return result;
""")

fun box(): String {
    assertEquals(6, sumValuesVar(arrayOf(1,2,3)))
    assertEquals(6, sumValuesConst(arrayOf(1,2,3)))
    assertEquals(6, sumValuesLet(arrayOf(1,2,3)))
    assertEquals(6, sumValuesExisting(arrayOf(1,2,3)))
    assertEquals(6, sumValuesInExpression(arrayOf(1,2,3)))

    return "OK"
}