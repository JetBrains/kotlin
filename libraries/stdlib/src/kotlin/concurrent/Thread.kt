package kotlin.concurrent

import java.util.concurrent.*

public val currentThread: Thread
    get() = Thread.currentThread()

public var Thread.name: String
    get() = getName()
    set(value) {
        setName(value)
    }

public var Thread.daemon: Boolean
    get() = isDaemon()
    set(value) {
        setDaemon(value)
    }

public val Thread.alive: Boolean
    get() = isAlive()

public var Thread.priority: Int
    get() = getPriority()
    set(value) {
        setPriority(value)
    }

public var Thread.contextClassLoader: ClassLoader?
    get() = getContextClassLoader()
    set(value) {
        setContextClassLoader(value)
    }

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
 * execute the given block on the [[Executor]].
 */
public fun Executor.invoke(action: () -> Unit) {
    execute(action)
}

/**
 * Allows you to use the executor as a function to
 * execute the given block on the [[Executor]].
 */
public fun <T> ExecutorService.invoke(action: () -> T): Future<T> {
    return submit(action)
}