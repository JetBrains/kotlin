inline fun <reified T> getTypeName(s: T): String {
    return "${T::class}"
}
