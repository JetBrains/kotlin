package usage
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun async(x: suspend Controller.() -> Unit) {
    x.startCoroutine(Controller(), object : Continuation<Unit> {
        override val context: CoroutineContext = null!!
        override fun resumeWith(result: Result<Unit>) {}
    })
}

class Controller {
    suspend fun step(param: Int) = suspendCoroutineUninterceptedOrReturn<Int> { next ->
        next.resume(param + 1)
    }
}

fun bar() {
    async {
        val result = step(1)
        inline.f(result)
    }
}
