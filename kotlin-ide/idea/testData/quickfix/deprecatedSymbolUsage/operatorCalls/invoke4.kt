// "Replace with 'execute(action)'" "true"

class Executor {
    val self: Executor
        get() = this

    @Deprecated("Use Executor.execute(Runnable) instead.", ReplaceWith("execute(action)"))
    operator fun invoke(action: () -> Unit) {}

    fun execute(action: () -> Unit) {}
}

fun usage(executor: Executor) {
    executor.<caret>self {
        // do something
    }
}