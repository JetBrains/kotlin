package sample

// Keep `target` and add a higher-arity suspend function: additionally loads
// SuspendFunction3 (<: Function4).
suspend fun target(x: Long, y: Int, z: Int): Int = (x + y + z).toInt()

suspend fun target2(x: Long, y: Int): Int = (x + y).toInt()
