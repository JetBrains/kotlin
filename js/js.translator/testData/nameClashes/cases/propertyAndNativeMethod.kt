package foo

@native fun bar()

val bar = 32

fun createNativeBar() = js("bar = function() { return 23; };")

fun box(): String {
    createNativeBar()

    assertEquals(23, bar())
    assertEquals(32, bar)

    return "OK"
}