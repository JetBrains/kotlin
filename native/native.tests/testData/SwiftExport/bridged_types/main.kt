fun identity(obj: Any): Any = obj

fun produceAny(): Any {
    return Any()
}
fun equals(any1: Any, any2: Any): Boolean {
    return any1 == any2
}

fun refEquals(any1: Any, any2: Any): Boolean {
    return any1 === any2
}
