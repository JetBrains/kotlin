package kotlin.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Callable

val currentThread : Thread
    get() = Thread.currentThread()

var Thread.name : String
    get() = getName()
    set(name: String) { setName(name) }

var Thread.daemon : Boolean
    get() = isDaemon()
    set(on: Boolean) { setDaemon(on) }

val Thread.alive : Boolean
    get() = isAlive()

var Thread.priority : Int
    get() = getPriority()
    set(prio: Int) { setPriority(prio) }

var Thread.contextClassLoader : ClassLoader?
    get() = getContextClassLoader()
    set(loader: ClassLoader?) { setContextClassLoader(loader) }

public fun thread(start: Boolean = true, daemon: Boolean = false, contextClassLoader: ClassLoader? = null, name: String? = null, priority: Int = -1, block: ()->Unit) : Thread {
    val thread = object: Thread() {
        public override fun run() {
            block()
        }
    }
    if(daemon)
        thread.setDaemon(true)
    if(priority > 0)
        thread.setPriority(priority)
    if(name != null)
        thread.setName(name)
    if(contextClassLoader != null)
        thread.setContextClassLoader(contextClassLoader)
    if(start)
        thread.start()
    return thread
}

/**
 * Allows you to use the executor as a function to
 * execute the given block on the [[Executor]].
 */
public /*inline*/ fun Executor.invoke(action: ()->Unit) {
    execute(runnable(action))
}

/**
* Allows you to use the executor as a function to
* execute the given block on the [[Executor]].
*/
public /*inline*/ fun <T>ExecutorService.invoke(action: ()->T):Future<T> {
    return submit(action)
}