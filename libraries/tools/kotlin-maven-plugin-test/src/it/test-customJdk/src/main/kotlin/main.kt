// available in JDK 1.8
fun <T> java.util.stream.Stream<T>.count(): Int {
    return 0
}

// available since JDK 9, should be an error with JDK 1.8
fun java.lang.StackWalker.doSomething() {}