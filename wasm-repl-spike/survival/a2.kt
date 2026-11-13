// Provider module: declares Q and exports (via binary patch) functions with
// user-class signatures, so the importing module can be supplied with them
// as @WasmImport("host", ...) imports through JS.

class Q(val v: Int) {
    open fun doubled(): Int = v * 2
}

fun makeQ(v: Int): Q = Q(v)

fun readQ(q: Q): Int = q.v * 10 + q.doubled()

fun isQ(x: Any?): Boolean = x is Q
