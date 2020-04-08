package soInlineOperatorIterator

fun main(args: Array<String>) {
    //Breakpoint!
    val list = listOf("a", "b", "c")
    for (element in Some(list)) { // No inlining visible on this string
        nonInline(element)
    }
}

fun <T> nonInline(p: T): T = p

class Some<T>(val list: List<T>) {
    operator fun iterator() = SomeIterator(list)
}

class SomeIterator<T>(list: List<T>) {
    val iterator = list.iterator()

    inline operator fun hasNext() : Boolean {
        return iterator.hasNext()
    }

    inline operator fun next(): T {
        return iterator.next()
    }
}

// STEP_OVER: 15
