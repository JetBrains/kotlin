// FIX: Wrap call with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() {}

suspend fun CoroutineScope.foo() {
    async {
        calcSomething()
    }
    <caret>async {
        calcSomething()
    }
}