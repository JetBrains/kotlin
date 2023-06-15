import kotlin.native.ref.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun foo(x: Int) {
    createCleaner(42) { println(x) }
}
