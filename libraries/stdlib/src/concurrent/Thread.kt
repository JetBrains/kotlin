package kotlin.concurrent

import java.util.concurrent.Executor

inline val currentThread : Thread
    get() = Thread.currentThread().sure()

inline var Thread.name : String
    get() = getName().sure()
    set(name: String) { setName(name) }

inline var Thread.daemon : Boolean
    get() = isDaemon()
    set(on: Boolean) { setDaemon(on) }

inline val Thread.alive : Boolean
    get() = isAlive()

inline var Thread.priority : Int
    get() = getPriority()
    set(prio: Int) { setPriority(prio) }

inline var Thread.contextClassLoader : ClassLoader?
    get() = getContextClassLoader()
    set(loader: ClassLoader?) { setContextClassLoader(loader) }

fun thread(start: Boolean = true, daemon: Boolean = false, contextClassLoader: ClassLoader? = null, name: String? = null, priority: Int = -1, block: ()->Unit) : Thread {
    val thread = object: Thread() {
        override fun run() {
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

inline fun Executor.execute(action: ()->Unit) {
    execute(object: Runnable{
        override fun run() {
            action()
        }
    })
}
