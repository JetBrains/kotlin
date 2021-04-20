import java.lang.Runnable
import java.lang.Thread

val topRunnable = object : Runnable {
    override fun run() {
        println("I'm running")
    }
}

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
