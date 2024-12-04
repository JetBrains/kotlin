import cinterop.dummyFromCompilerOption
import cinterop.dummyFromDefFileHeaders
import cinterop.dummyFromIncludeDirs

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun nativeMainUsingCInterop() = dummyFromCompilerOption()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun nativeMainUsingCInterop2() = dummyFromIncludeDirs()

fun main() {

}