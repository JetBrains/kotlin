// SNIPPET

val x: Pair<Any?, Any?> = "hello" to null
fun Any.string() = this as String

// SNIPPET

// KT-10001: each snippet is a separate module, so no smart cast on a public API property of a previous snippet
if (x.first != null) <!SMARTCAST_IMPOSSIBLE!>x.first<!>.string()
