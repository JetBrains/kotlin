// Baseline: direct recursion (what ir2wasm slot mode's call sites would
// degenerate to at worst, and what plain compilation gives).
import kotlin.time.measureTime

fun fib(n: Int): Int = if (n < 2) n else fib(n - 1) + fib(n - 2)

fun main() {
    val t = measureTime { println(fib(34)) }
    println("direct:  $t")
}
