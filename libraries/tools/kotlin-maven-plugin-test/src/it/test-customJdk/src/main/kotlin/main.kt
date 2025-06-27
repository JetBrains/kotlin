// available in JDK 1.8
fun <T> java.util.stream.Stream<T>.count(): Int {
    return 0
}

// available in JDK 1.9, should be an error with JDK 1.8
fun emptyJavaList() = java.util.List.of()
fun strippedJavaString(str: String) = java.lang.String(str).strip()