package sample

// Same name and arity as step 2, but different parameter types (Long instead of Int): the functional
// interface arity is unchanged (SuspendFunction2/Function3), only the type arguments differ.
suspend fun target(x: Long, y: Int): Int = (x + y).toInt()
