class Klass<T>

fun test(obj: Any): String {
    return <caret>when {
        obj is String -> "string"
        obj is Int -> "int"
        obj is Klass<*> -> "class"
        else -> "unknown"
    }
}
