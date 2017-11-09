// WITH_RUNTIME

annotation class NoArg

class Simple(val a: String)

@NoArg
class Test(val a: String) {
    val x = 5
    val y: Simple? = Simple("Hello, world!")
}

fun box(): String {
    try {
        val test = Test::class.java.newInstance()

        if (test.x != 0) {
            return "Bad 5"
        }

        if (test.y != null) {
            return "Bad Hello, world!"
        }

        return "OK"
    } catch (e: Throwable) {
        e.printStackTrace()
        return "Fail"
    }
}