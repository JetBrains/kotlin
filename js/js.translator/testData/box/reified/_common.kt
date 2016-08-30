package foo

inline fun <reified T> isInstance(x: Any?): Boolean =
        x is T
