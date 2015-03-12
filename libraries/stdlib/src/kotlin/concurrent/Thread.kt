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
public var Thread.name: String
    get() = getName()
    set(value) {
        setName(value)
    }

/**
 * Exposes the daemon flag of this thread as a property.
 * The Java Virtual Machine exits when the only threads running are all daemon threads.
 */
public var Thread.daemon: Boolean
    get() = isDaemon()
    set(value) {
        setDaemon(value)
    }

/**
 * Exposes the alive state of this thread as a property.
 */
public val Thread.alive: Boolean
    get() = isAlive()

/**
 * Exposes the priority of this thread as a property.
 */
public var Thread.priority: Int
    get() = getPriority()
    set(value) {
        setPriority(value)
    }

/**
 * Exposes the context class loader of this thread as a property.
 */
public var Thread.contextClassLoader: ClassLoader?
    get() = getContextClassLoader()
    set(value) {
        setContextClassLoader(value)
    }

/**
 * Creates a thread that runs the specified [block] of code.\
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
 * Allows you to use the executor as a function to
 * execute the given block on the [Executor].
 */
public fun Executor.invoke(action: () -> Unit) {
    execute(action)
}

/**
 * Allows you to use the executor service as a function to
 * execute the given block on the [ExecutorService].
 */
public fun <T> ExecutorService.invoke(action: () -> T): Future<T> {
    return submit(action)
}