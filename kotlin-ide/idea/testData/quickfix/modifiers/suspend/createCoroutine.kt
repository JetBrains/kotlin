// "Make block type suspend" "true"
// WITH_RUNTIME

import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.coroutines.experimental.createCoroutine

suspend fun <T> suspending(): T {
    val block: () -> T = { null!! }
    return suspendCoroutine { block.<caret>createCoroutine(it) }
}