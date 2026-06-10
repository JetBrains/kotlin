// COMPILER_PLUGIN: allopen-compiler-plugin.jar annotation=AllOpen

annotation class AllOpen

@AllOpen
class Base {
    fun foo() {}
}

class Derived : Base()

fun box(): String {
    return "OK"
}
