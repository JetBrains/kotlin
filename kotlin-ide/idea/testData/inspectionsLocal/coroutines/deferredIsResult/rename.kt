// WITH_RUNTIME
// FIX: Rename to 'myFunctionAsync'

package kotlinx.coroutines

fun <caret>myFunction(): Deferred<Int> {
    return GlobalScope.async { 42 }
}
