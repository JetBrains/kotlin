// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test() {
    GlobalScope.<caret>async() { 42 }.await()
}