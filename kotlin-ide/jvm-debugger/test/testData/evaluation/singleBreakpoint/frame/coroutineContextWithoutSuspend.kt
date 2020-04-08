package coroutineContextWithoutSuspend

import kotlin.coroutines.coroutineContext

suspend fun main() {
    foo()
}

private suspend fun foo() {
    //Breakpoint!
    val a = 5
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME

// EXPRESSION: coroutineContext
// RESULT: instance of kotlin.coroutines.EmptyCoroutineContext(id=ID): Lkotlin/coroutines/EmptyCoroutineContext;