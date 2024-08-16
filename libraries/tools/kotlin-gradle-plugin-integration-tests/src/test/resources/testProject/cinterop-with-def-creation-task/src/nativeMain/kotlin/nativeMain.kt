import my.cinterop.dummy
import my.cinterop.foo

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun nativeMainUsingCInterop() = dummy()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main() {
    println(foo())
}