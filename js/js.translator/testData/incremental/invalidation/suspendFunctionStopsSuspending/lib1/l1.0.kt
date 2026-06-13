import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun foo(): Int = suspendCoroutine { continuation ->
    continuation.resume(41)
}
