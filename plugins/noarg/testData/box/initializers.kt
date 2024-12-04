// WITH_STDLIB
// INVOKE_INITIALIZERS

annotation class NoArg

class Simple(val a: String)

@NoArg
class Test(val a: String) {
    val x = 5
    val y = Simple("Hello, world!")
    val z by lazy { "TEST" }
}

fun box(): String {
    val test = Test::class.java.newInstance()

    if (test.x != 5) {
        return "Bad 5"
    }

    if (test.y == null || test.y.a != "Hello, world!") {
        return "Bad Hello, world!"
    }

    if (test.z != "TEST") {
        return "Bad TEST"
    }

    return "OK"
}
