import kotlinx.cinterop.*
import kotlin.native.Platform

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    StableRef.create(Any())
}
