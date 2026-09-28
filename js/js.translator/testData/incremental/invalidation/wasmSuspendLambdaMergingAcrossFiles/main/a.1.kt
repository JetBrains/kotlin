// The same lambda plus an unrelated declaration: only a.kt is recompiled on this step.
// `equivalentTypes` should be correctly obtained from caches, so that the compiler will
// understand, that one equivalent types is already presented.

fun a(x: Any): suspend () -> Any = { id(x); x }

fun unrelated() {}
