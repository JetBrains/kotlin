// WITH_RUNTIME

annotation class NoArg

@NoArg
class Test(val a: String)

fun box(): String {
    Test::class.java.newInstance()
    return "OK"
}
