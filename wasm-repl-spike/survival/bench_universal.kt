// Steady-state cost of universal-instance field access (post-migration):
//   direct:    s += xs[i].a
//   universal: s += (cells[i].p as P).a   — cell load + version cast + field
// Walks a 100k-element array repeatedly so loads stay non-invariant.
// This is the per-access price of CL-mutable classes on wasm GC: the class's
// canonical type is a stable CELL; versioned payloads hide behind a mutable
// field; access re-checks the payload version (lazily migrating old ones).
import kotlin.time.measureTime

class D(val a: Int, val b: Int)
class C(var p: Any)
class P(val a: Int, val b: Int)

fun main() {
    val n = 100_000
    val reps = 200
    val ds = Array(n) { D(it and 7, 1) }
    val cs = Array(n) { C(P(it and 7, 1)) }
    var s1 = 0
    val t1 = measureTime { repeat(reps) { for (i in 0 until n) s1 += ds[i].a } }
    println("direct:     $t1 ($s1)")
    var s2 = 0
    val t2 = measureTime { repeat(reps) { for (i in 0 until n) s2 += (cs[i].p as P).a } }
    println("universal:  $t2 ($s2)")
}
