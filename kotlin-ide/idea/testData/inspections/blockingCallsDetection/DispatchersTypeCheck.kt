@file:Suppress("UNUSED_PARAMETER")
package kotlinx.coroutines

import java.lang.Thread.sleep

suspend fun withIoDispatcher() {
    withContext(Dispatchers.IO) {
        //no warning since IO dispatcher type used
        Thread.sleep(42)
    }

    withContext(Dispatchers.Default) {
        Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(1)
    }
}

class CoroutineDispatcher()
object Dispatchers {
    val IO: kotlinx.coroutines.CoroutineDispatcher = TODO()
    val Default: kotlinx.coroutines.CoroutineDispatcher = TODO()
}

suspend fun <T> withContext(
    context: CoroutineDispatcher,
    f: suspend () -> T
) {
    TODO()
}