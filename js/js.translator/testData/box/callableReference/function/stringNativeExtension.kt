// WITH_STDLIB
package foo

fun box(): String {
    var s = "abc"
    assertEquals("ABC", (String::uppercase)(s))

    return "OK"
}
