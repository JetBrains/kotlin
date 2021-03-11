// FIX: Remove receiver & wrap with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

suspend fun <caret>CoroutineScope.foo() {
    val deferred = async {
        calcSomething()
    }
}