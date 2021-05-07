import java.lang.Runnable
import java.lang.Thread

// file comment

// Single-line comment bound to top-level property
val topRunnable = object : Runnable {
    override fun run() {
        println("I'm running")
    }
}

// Single-line comment bound to object
object RunnableManager {
    val tasks : MutableList<Runnable> = mutableListOf<Runnable>()

    fun register(runnable : Runnable) {
        tasks.add(runnable)
    }

    fun runAll() {
        for (t : tasks) {
            Thread(t).start()
        }
    }
}

fun main() {
    RunnableManager.register(topRunnable)
    RunnableManager.runAll()
}
