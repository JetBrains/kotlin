// WITH_STDLIB

annotation class NoArg

@NoArg
class Foo(val s1: String) {
    val s2: String = ""
    val l: List<String> = listOf()
}

fun box(): String {
    val instance = Foo::class.java.newInstance()
    return "OK"
}