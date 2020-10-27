import kotlinx.cinterop.*

class Z {
    fun foo(x: Int) = x
}

fun bar() {
    staticCFunction(Z()::foo)
}
