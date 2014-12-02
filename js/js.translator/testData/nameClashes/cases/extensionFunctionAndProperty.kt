package foo

fun box(): String {

    val data = listOf("foo", "bar")

    assertEquals("bar", data.last())
    assertEquals("bar", data.last)

    assertEquals("foo", data.first)
    assertEquals("foo", data.first())

    return "OK"
}