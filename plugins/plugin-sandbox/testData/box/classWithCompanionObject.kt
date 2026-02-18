import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
class SomeClass

fun takeInt(x: Int) {
    if (x != 10) throw IllegalArgumentException()
}

fun test() {
    takeInt(SomeClass.foo())
    takeInt(SomeClass.Companion.foo())
}

fun box(): String {
    test()
    return "OK"
}

