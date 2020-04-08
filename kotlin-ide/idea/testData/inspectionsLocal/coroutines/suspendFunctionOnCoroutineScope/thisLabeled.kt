// FIX: Remove receiver & wrap with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() {}

class My {
    suspend fun <caret>CoroutineScope.foo() {
        this@foo.async {
            calcSomething()
        }
    }
}