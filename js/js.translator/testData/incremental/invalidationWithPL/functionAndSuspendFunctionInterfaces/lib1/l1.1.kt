package sample

// `suspend` removed (same name/arity, pure-function counterpart). The SuspendFunction1/Function2
// supertype for `target` is no longer synthesized; a stale suspend reference in a not-recompiled
// consumer becomes a partial-linkage stub.
fun target(x: Int): Int = x + 1
