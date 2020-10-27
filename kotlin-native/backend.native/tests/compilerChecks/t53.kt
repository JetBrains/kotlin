import kotlin.native.internal.*

class C(val x: Int) {
    fun bar(y: Int) = println(x + y)
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun foo(x: Int) {
    createCleaner(42, C(x)::bar)
}
