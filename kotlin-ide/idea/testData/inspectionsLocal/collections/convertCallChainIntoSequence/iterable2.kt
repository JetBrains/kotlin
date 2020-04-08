// WITH_RUNTIME

class A<T>(private val list: List<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = list.iterator()
}

fun test(i: A<Int>): List<Int> {
    return i.<caret>filter { it > 1 }.map { it * 2 }
}