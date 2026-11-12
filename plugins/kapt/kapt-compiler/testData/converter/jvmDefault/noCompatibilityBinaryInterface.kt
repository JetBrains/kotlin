// JVM_DEFAULT_MODE: no-compatibility
// WITH_STDLIB

import kotlin.coroutines.CoroutineContext

class Holder : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Key

    companion object Key : CoroutineContext.Key<Holder>
}

interface TransactionContextHolder : CoroutineContext.Element {
    val transaction: String?
}
