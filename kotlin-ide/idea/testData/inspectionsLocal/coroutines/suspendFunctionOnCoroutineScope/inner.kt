// PROBLEM: none

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineContext

fun use(context: CoroutineContext) {}

abstract class MyCoroutineScope : CoroutineScope {
    inner class Inner {
        suspend fun foo() {
            // Does not work yet (could work)
            use(<caret>coroutineContext)
        }
    }
}