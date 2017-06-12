// EXPECTED_REACHABLE_NODES: 499
package foo

data class A(val value: Int)

fun box(): String {
    assertEquals(-1, js("-1"), "- (unary)")
    assertEquals(1, js("+'1'"), "+ (unary)")

    assertEquals(1, js("2 - 1"), "- (binary)")
    assertEquals(3, js("2 + 1"), "+ (binary)")
    assertEquals(10, js("2 * 5"), "*")
    assertEquals(5, js("10 / 2"), "/")
    assertEquals(1, js("11 % 2"), "%")

    assertEquals(36, js("9 << 2"), "<<")
    assertEquals(2, js("9 >> 2"), ">>")
    assertEquals(1073741821, js("-9 >>> 2"), ">>>")
    assertEquals(0, js("0 & 1"), "&")
    assertEquals(1, js("0 | 1"), "|")
    assertEquals(1, js("0 ^ 1"), "^")
    assertEquals(-2, js("~1"), "~")

    var i = 2
    assertEquals(1, js("--i"), "-- prefix")
    assertEquals(1, js("i--"), "-- postfix (1)")
    assertEquals(0, js("i"), "-- postfix (0)")
    assertEquals(1, js("++i"), "++ prefix")
    assertEquals(1, js("i++"), "++ postfix (1)")
    assertEquals(2, js("i"), "++ postfix (0)")

    assertEquals(true , js("true || false"), "||")
    assertEquals(false , js("true && false"), "&&")
    assertEquals(false , js("!true"), "!")

    assertEquals(true , js("1 < 2"), "<")
    assertEquals(false, js("1 > 2"), ">")
    assertEquals(true, js("1 <= 2"), "<=")
    assertEquals(false, js("1 >= 2"), ">=")

    assertEquals(false, js("2 === '2'"), "===")
    assertEquals(true, js("2 !== '2'"), "!==")
    assertEquals(true, js("2 == '2'"), "==")
    assertEquals(false, js("2 != '2'"), "!=")
    
    assertEquals("odd", js("(1 % 2 === 0)?'even':'odd'"), "?:")
    assertEquals("even", js("(4 % 2 === 0)?'even':'odd'"), "?:")
    assertEquals(3, js("1,2,3"), ", (comma)")

    var j = 0
    assertEquals(1, js("j = 1"), "=")
    assertEquals(3, js("j += 2"), "+=")
    assertEquals(2, js("j -= 1"), "-=")
    assertEquals(14, js("j *= 7"), "*=")
    assertEquals(7, js("j /= 2"), "/=")
    assertEquals(1, js("j %= 2"), "%=")

    assertEquals(undefined, js("(void 0)"), "void")
    assertEquals(true, js("'key' in {'key': 10}"), "in")
    assertEquals("string", js("typeof 'str'"), "typeof")
    assertEquals(A(2), js("new _.foo.A(2)"), "new")
    assertEquals(true, js("new String('str') instanceof String"), "instanceof")

    var s: Any = js("({key: 10})")
    assertEquals(undefined, js("delete s.key, s.key"), "delete")

    return "OK"
}
