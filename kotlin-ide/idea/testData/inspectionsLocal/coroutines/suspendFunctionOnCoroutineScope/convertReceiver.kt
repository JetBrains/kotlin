// FIX: Convert receiver to parameter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

suspend fun <caret>CoroutineScope.foo() = async { calcSomething() }