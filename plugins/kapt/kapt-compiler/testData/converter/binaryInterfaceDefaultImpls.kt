// WITH_STDLIB

import kotlin.coroutines.CoroutineContext

interface TransactionContextHolder : CoroutineContext.Element {
    val transaction: String?
}
