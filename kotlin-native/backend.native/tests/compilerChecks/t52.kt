import kotlin.native.internal.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun foo(x: Int) {
    createCleaner(42) { println(x) }
}
