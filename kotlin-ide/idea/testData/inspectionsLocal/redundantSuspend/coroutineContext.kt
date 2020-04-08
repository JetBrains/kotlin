// PROBLEM: none
// WITH_RUNTIME

import kotlin.coroutines.coroutineContext

<caret>suspend fun test() {
    coroutineContext
}