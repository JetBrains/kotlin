open class ScopeCoroutine<T> {
    val callerFrame: Any? = null
}

expect class UndispatchedCoroutine<T>(): ScopeCoroutine<T>
