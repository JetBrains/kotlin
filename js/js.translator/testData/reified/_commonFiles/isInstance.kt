package foo

inline fun isInstance<reified T>(x: Any): Boolean =
        x is T