// PROBLEM: none
// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    coroutineScope {
        <caret>async(start = CoroutineStart.LAZY) { 42 }.await()
    }
}