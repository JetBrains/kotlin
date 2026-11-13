// The exact lrepl slot lowering: a mutable global of function type holding
// the implementation, invoked through the Function1 interface on every call.
import kotlin.time.measureTime

var fib__slot: (Int) -> Int = { n: Int -> if (n < 2) n else fib(n - 1) + fib(n - 2) }
fun fib(n: Int): Int = fib__slot(n)

fun main() {
    val t = measureTime { println(fib(34)) }
    println("slot:    $t")
}
