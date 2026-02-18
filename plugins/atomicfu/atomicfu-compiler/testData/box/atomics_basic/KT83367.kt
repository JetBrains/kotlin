// MODULE: lib
// FILE: lib.kt
import kotlinx.atomicfu.*

private class HolderWrapper(val any: Any) {
    val flag = atomic(false)
}

public class Accessor {
    public fun access() {
        val h = HolderWrapper("")
        h.flag.compareAndSet(false, true)
    }
}

// MODULE: app(lib)
// FILE: app.kt
fun box(): String {
    val a = Accessor()
    return "OK"
}
