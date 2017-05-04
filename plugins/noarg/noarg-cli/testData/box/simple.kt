// WITH_RUNTIME

annotation class NoArg

@NoArg
class Test(val a: String)

fun box(): String {
    try {
        Test::class.java.newInstance()
        return "OK"
    } catch (_: Throwable) {
        return "Fail"
    }
}