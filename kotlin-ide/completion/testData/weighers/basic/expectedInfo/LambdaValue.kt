fun <T, R> List<T>.map(transform: (T) -> R): List<R> {}

class X {
    val x1: String
    val x2: Any
    val x3: Int
}

fun foo(list: List<X>): Collection<Int> {
    return list.map { it.x<caret> }
}

// ORDER: x3
// ORDER: x1
// ORDER: x2
