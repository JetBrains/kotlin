// WITH_STDLIB

annotation class NoArg

class Outer {
    @NoArg
    class Nested(val a: String)
}

fun box(): String {
    Outer.Nested::class.java.newInstance()

    return "OK"
}
