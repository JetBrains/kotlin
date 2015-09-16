@file:JvmVersion
@file:JvmName("ThreadsKt")
package kotlin.concurrent

import java.util.concurrent.*

/**
 * Returns the current thread.
 */
public val currentThread: Thread
    get() = Thread.currentThread()

/**
 * Exposes the name of this thread as a property.
 */
@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("name"))
public var Thread.name: String
    get() = getName()
    set(value) {
        setName(value)
    }

/**
 * Exposes the daemon flag of this thread as a property.
 * The Java Virtual Machine exits when the only threads running are all daemon threads.
 */
@Deprecated("Use synthetic extension property isDaemon instead.", ReplaceWith("this.isDaemon"))
public var Thread.daemon: Boolean
    get() = isDaemon()
    set(value) {
        setDaemon(value)
    }

/**
 * Exposes the alive state of this thread as a property.
 */
@Deprecated("Use synthetic extension property isAlive instead.", ReplaceWith("this.isAlive"))
public val Thread.alive: Boolean
    get() = isAlive()

/**
 * Exposes the priority of this thread as a property.
 */
@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("priority"))
public var Thread.priority: Int
    get() = getPriority()
    set(value) {
        setPriority(value)
    }

/**
 * Exposes the context class loader of this thread as a property.
 */
@HiddenDeclaration
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("contextClassLoader"))
public var Thread.contextClassLoader: ClassLoader?
    get() = getContextClassLoader()
    set(value) {
        setContextClassLoader(value)
    }

/**
 * Creates a thread that runs the specified [block] of code.
 *
 * @param start if `true`, the thread is immediately started.
 * @param daemon if `true`, the thread is created as a daemon thread. The Java Virtual Machine exits when
 * the only threads running are all daemon threads.
 * @param contextClassLoader the class loader to use for loading classes and resources in this thread.
 * @param name the name of the thread.
 * @param priority the priority of the thread.
 */
public fun thread(start: Boolean = true, daemon: Boolean = false, contextClassLoader: ClassLoader? = null, name: String? = null, priority: Int = -1, block: () -> Unit): Thread {
    val thread = object : Thread() {
        public override fun run() {
            block()
        }
    }
    if (daemon)
        thread.setDaemon(true)
    if (priority > 0)
        thread.setPriority(priority)
    if (name != null)
        thread.setName(name)
    if (contextClassLoader != null)
        thread.setContextClassLoader(contextClassLoader)
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
public inline fun <T: Any> ThreadLocal<T>.getOrSet(default: () -> T): T {
    // TODO: replace let with apply or touch
    return get() ?: default().let { set(it); it }
}

/**
 * Allows you to use the executor as a function to
 * execute the given block on the [Executor].
 */
@Deprecated("Use Executor.execute(Runnable) instead.")  // do not specify ReplaceWith("execute(action)") due to KT-8597
public operator fun Executor.invoke(action: () -> Unit) {
    execute(action)
}

/**
 * Allows you to use the executor service as a function to
 * execute the given block on the [ExecutorService].
 */
@Deprecated("Use ExecutorService.submit(Callable) instead.")  // do not specify ReplaceWith("submit(action)") due to KT-8597
public operator fun <T> ExecutorService.invoke(action: () -> T): Future<T> {
    return submit(action)
}
