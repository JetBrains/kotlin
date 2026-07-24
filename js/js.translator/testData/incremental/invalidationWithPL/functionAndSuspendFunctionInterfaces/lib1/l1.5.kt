package sample

// `target` removed: the arity-3 functional interfaces are no longer referenced here, and a stale
// `::target` reference in a not-recompiled consumer becomes a partial-linkage stub.
suspend fun target2(x: Long, y: Int): Int = (x + y).toInt()
