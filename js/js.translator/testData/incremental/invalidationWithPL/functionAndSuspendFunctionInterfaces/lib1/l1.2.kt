package sample

// `suspend` re-added, arity bumped to 2: loads SuspendFunction2 (<: Function3)
suspend fun target(x: Int, y: Int): Int = x + y
