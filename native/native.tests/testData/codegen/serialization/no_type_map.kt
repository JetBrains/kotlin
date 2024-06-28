import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
fun box(): String {
    memScoped {
        val bufferLength = 100L
        val buffer = allocArray<ByteVar>(bufferLength)
    }
    return "OK"
}