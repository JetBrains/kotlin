package kt50786

import kotlinx.cinterop.*
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

lateinit var continuation: Continuation<Unit>

suspend fun suspendHere(): Unit = suspendCoroutine { cont ->
    continuation = cont
}


fun startCoroutine(block: suspend () -> Unit) {
    block.startCoroutine(EmptyContinuation)
}

@kotlin.test.Test
fun testSafeSuspensionIsAllowedInAutoreleasepool() {
    // This test checks that the compiler doesn't prohibit calling suspend functions from `autoreleasepool {}`
    // if this call is not actually in the block, but in the local declaration inside it.
    // See https://youtrack.jetbrains.com/issue/KT-50786 for more details.
    autoreleasepool {
        startCoroutine {
            suspendHere()
        }
    }

    continuation.resume(Unit)
}
