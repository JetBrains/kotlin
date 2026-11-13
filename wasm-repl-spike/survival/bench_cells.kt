// Real cell-encoding cost gate: field access through the compiler-generated
// payload indirection (struct.get cell + ref.cast payload + struct.get field)
// vs plain (struct.get field). Same source built without/with
// -Xwasm-hot-reload; run each and compare. Measured 2026-11-11 (M1, V8):
//   plain 68.5 ms vs cell+cast 118 ms over 20M accesses  ~= 2.5 ns/access
// (the earlier ~1 ns from bench_universal was a FINAL-exact-cast hand
// simulation; the real encoding pays for the open payload cast too).
// While-loops keep the slot machinery out of the measurement.
import kotlin.time.measureTime

class D(val a: Int, val b: Int)

fun main() {
    val n = 100_000
    val reps = 200
    val ds = Array(n) { D(it and 7, 1) }
    var s = 0
    val t = measureTime {
        var r = 0
        while (r < reps) {
            var i = 0
            while (i < n) { s += ds[i].a; i++ }
            r++
        }
    }
    println("field loop: $t ($s)")
}
