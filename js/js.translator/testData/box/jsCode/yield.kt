package foo

fun box(): String {
    val generator = js("""(
        function*() {
            yield "1";
            yield;
            var value = yield 2;
            var undef = yield;
        }
    )""")

    val sequence = generator()
    assertEquals("1", sequence.next().value)
    assertEquals(js("undefined"), sequence.next().value)
    assertEquals(2, sequence.next().value)
    assertEquals(js("undefined"), sequence.next().value)

    return "OK"
}