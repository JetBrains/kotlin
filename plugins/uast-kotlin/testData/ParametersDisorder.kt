fun global(a: Int, b: Float) {}

fun withDefault(c: Int = 1, d: String = "aaa") {}

fun String.withReceiver(a: Int, b: Float) {}

fun call() {
    global(b = 2.2F, a = 2)
    withDefault(d = "bbb")
    "abc".withReceiver(1, 1.2F)
    Math.atan2(1.3, 3.4)
    unresolvedMethod("param1", "param2")
    java.lang.String.format("%i %i %i", 1, 2, 3)
    java.lang.String.format("%i %i %i", arrayOf(1, 2, 3))
    java.lang.String.format("%i %i %i", arrayOf(1, 2, 3), arrayOf(4, 5, 6))
    java.lang.String.format("%i %i %i", *"".chunked(2).toTypedArray())

    with(A()) {
        "def".with2Receivers(8, 7.0F)
    }

}

class A {

    fun String.with2Receivers(a: Int, b: Float) {}

}

open class Parent(a: String, b: Int)

fun objectLiteral() {

    object : Parent(b = 1, a = "foo") { }
}
