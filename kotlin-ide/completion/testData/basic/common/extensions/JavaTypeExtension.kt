fun <T> Iterable<T>.first() : T? {
    return this.iterator()?.next()
}

fun main(args : Array<String>) {
    val test = HashSet<Int>() // aliased in JVM to java.util.HashSet
    test.<caret>
}

// EXIST: first
