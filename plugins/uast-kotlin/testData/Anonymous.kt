import java.io.Closeable
import java.io.InputStream


fun foo() {
    val runnable = object : Runnable { override fun run() {} }
    runnable.run()
    val runnable2 = Runnable { println() }
    runnable2.run()
    val closeableRunnable = object : Runnable, Closeable { override fun close() {} override fun run() {} }
    val runnableIs = object : InputStream(), Runnable { override fun read(): Int = 0; override fun run() {} }
}

fun withErr() {
    val runnable = object  Runnable {} // EA-122644
}