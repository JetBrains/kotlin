// available in JDK 1.7
fun java.lang.AutoCloseable.silentClose() {

}

// available in JDK 1.8, should be an error with JDK 1.7
fun <T> java.util.stream.Stream<T>.count(): Int {
    return 0
}