@file:JvmVersion
@file:JvmName("ThreadsKt")
package kotlin.concurrent

/**
 * Creates a thread that runs the specified [block] of code.
 *
 * @param start if `true`, the thread is immediately started.
 * @param isDaemon if `true`, the thread is created as a daemon thread. The Java Virtual Machine exits when
 * the only threads running are all daemon threads.
 * @param contextClassLoader the class loader to use for loading classes and resources in this thread.
 * @param name the name of the thread.
 * @param priority the priority of the thread.
 */
public fun thread(start: Boolean = true, isDaemon: Boolean = false, contextClassLoader: ClassLoader? = null, name: String? = null, priority: Int = -1, block: () -> Unit): Thread {
    val thread = object : Thread() {
        public override fun run() {
            block()
        }
    }
    if (isDaemon)
        thread.isDaemon = true
    if (priority > 0)
        thread.priority = priority
    if (name != null)
        thread.name = name
    if (contextClassLoader != null)
        thread.contextClassLoader = contextClassLoader
    if (start)
        thread.start()
    return thread
}

/**
 * Gets the value in the current thread's copy of this
 * thread-local variable or replaces the value with the result of calling
 * [default] function in case if that value was `null`.
 *
 * If the variable has no value for the current thread,
 * it is first initialized to the value returned
 * by an invocation of the [ThreadLocal.initialValue] method.
 * Then if it is still `null`, the provided [default] function is called and its result
 * is stored for the current thread and then returned.
 */
@kotlin.internal.InlineOnly
public inline fun <T: Any> ThreadLocal<T>.getOrSet(default: () -> T): T {
    return get() ?: default().apply { set(this) }
}
