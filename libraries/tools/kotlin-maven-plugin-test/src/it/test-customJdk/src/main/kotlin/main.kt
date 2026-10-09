// available in JDK 17
fun <T> java.util.stream.Stream<T>.count(): Int {
    return 0
}

// available since JDK 21, should be an error with JDK 17
fun java.util.SequencedCollection<String>.doSomething() {}
