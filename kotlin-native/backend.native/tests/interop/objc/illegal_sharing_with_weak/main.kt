import kotlin.native.internal.*
import kotlin.test.*
import kotlin.native.concurrent.*
import kotlinx.cinterop.autoreleasepool
import objclib.*

fun main() {
    autoreleasepool {
        run()
    }
}

private class NSObjectImpl : NSObject() {
    var x = 111
}

fun run() = withWorker {
    val frame = runtimeGetCurrentFrame()
    val obj = NSObjectImpl()
    setObject(obj)

    println("Before")
    val isAlive = try {
        execute(TransferMode.SAFE, {}) {
            isObjectAliveShouldCrash()
        }.result
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        false
    }
    println("After $isAlive")
}
