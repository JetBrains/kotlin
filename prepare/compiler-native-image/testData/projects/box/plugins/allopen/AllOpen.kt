// COMPILER_PLUGIN: kotlin-allopen-compiler-plugin-2.4.20.jar annotation=AllOpen

annotation class AllOpen

@AllOpen
class Base {
    fun foo() {}
}

class Derived : Base()

fun box(): String {
    return "OK"
}
