inline fun <reified T> getTypeName(s: T): String {
    return "_${T::class}"
}
