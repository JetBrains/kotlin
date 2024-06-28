package lib

inline fun <reified T> safeCall(x: Any?, fn: (T) -> T): T? =
    if (x is T) fn(x) else null