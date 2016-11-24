package usage

fun async(coroutine x: Controller.() -> Continuation<Unit>) {
    x(Controller()).resume(Unit)
}

class Controller {
    suspend fun step(param: Int) = suspendWithCurrentContinuation<Int> { next ->
        next.resume(param + 1)
    }
}

fun bar() {
    async {
        val result = step(1)
        inline.f(result)
    }
}
