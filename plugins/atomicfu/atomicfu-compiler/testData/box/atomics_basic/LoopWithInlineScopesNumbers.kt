// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// CHECK_BYTECODE_TEXT
// KT-91570: the `loop$atomicfu$...` function that the plugin generates has no marker variable. When it is inlined,
// its variables must still get a scope number, and the lambda gets the call site of `loop`.

import kotlinx.atomicfu.*
import kotlin.test.*

class Box {
    private val state = atomic(0)

    fun waitPositive(): Int {
        state.loop { cur ->
            if (cur > 0) return cur
            state.compareAndSet(cur, cur + 1)
        }
    }
}

fun box(): String {
    assertEquals(1, Box().waitPositive())
    return "OK"
}

// 1 LOCALVARIABLE handler\$atomicfu\\1 L
// 1 LOCALVARIABLE \$i\$a\$-loop\$atomicfu\$ATOMIC_FIELD_UPDATER\$Int-Box\$waitPositive\$1\\2\\14\\0 I
