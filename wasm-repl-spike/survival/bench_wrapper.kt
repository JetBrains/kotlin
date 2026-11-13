// Wrapper layer only: an exported stable entry calling a direct impl
// (isolates the cost of one extra call frame + lazy-property machinery
// without the Function1 boxing).
import kotlin.time.measureTime

fun fibImpl(n: Int): Int = if (n < 2) n else fibImpl(n - 1) + fibImpl(n - 2)
fun fib(n: Int): Int = fibImpl(n)

fun main() {
    val t = measureTime { println(fib(34)) }
    println("wrapper: $t")
}
