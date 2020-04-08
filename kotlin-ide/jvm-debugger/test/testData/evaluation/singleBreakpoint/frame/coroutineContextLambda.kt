package coroutineContextLambda

import kotlin.coroutines.coroutineContext

suspend fun main() {
    foo()
}

private var foo: suspend () -> Unit = {
    bar()
}

private var bar: suspend () -> Unit = {
    //Breakpoint!
    val a = 5
}

// EXPRESSION: coroutineContext
// RESULT: instance of kotlin.coroutines.EmptyCoroutineContext(id=ID): Lkotlin/coroutines/EmptyCoroutineContext;