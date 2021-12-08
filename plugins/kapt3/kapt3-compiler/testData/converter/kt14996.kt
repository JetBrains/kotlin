// IGNORE_BACKEND: JVM_IR

fun crashMe(values: List<String>): String {
    throw UnsupportedOperationException()
}

fun crashMe(values: List<CharSequence>): CharSequence {
    throw UnsupportedOperationException()
}
