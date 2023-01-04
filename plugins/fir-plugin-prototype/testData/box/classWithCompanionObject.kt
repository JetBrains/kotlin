//FIR_DISABLE_LAZY_RESOLVE_CHECKS
import org.jetbrains.kotlin.fir.plugin.CompanionWithFoo

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

