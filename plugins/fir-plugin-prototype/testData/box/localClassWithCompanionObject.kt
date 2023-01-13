import org.jetbrains.kotlin.fir.plugin.CompanionWithFoo

fun box(): String {
    @CompanionWithFoo
    class SomeClass

    fun takeInt(x: Int) {
        if (x != 10) throw IllegalArgumentException()
    }

    fun test() {
        takeInt(SomeClass.foo())
        takeInt(SomeClass.Companion.foo())
    }

    test()
    return "OK"
}
