import org.jetbrains.kotlin.fir.plugin.D

@D
class SomeClass

fun takeInt(x: Int) {}

fun test() {
    takeInt(SomeClass.foo())
    takeInt(SomeClass.Companion.foo())
}


