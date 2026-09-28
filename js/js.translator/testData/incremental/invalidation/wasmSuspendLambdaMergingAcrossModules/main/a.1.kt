import sample.*

// The same lambda plus an unrelated declaration: only this file is recompiled on this step, while
// the fragments of lib1 are taken from the incremental cache.
fun fromMain(x: Any): suspend () -> Any = { id(x); x }

fun unrelated() {}
