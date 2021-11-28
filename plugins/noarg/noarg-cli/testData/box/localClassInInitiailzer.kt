// WITH_STDLIB
// INVOKE_INITIALIZERS

annotation class NoArg

@NoArg
class Test(val a: String) {
    val lc = run {
        class Local(val result: String)
        Local("OK").result
    }

    val obj = object { val result = "OK" }.result
}

fun box(): String {
    val t = Test::class.java.newInstance()
    if (t.lc != "OK") return "Fail 1"
    return t.obj
}
