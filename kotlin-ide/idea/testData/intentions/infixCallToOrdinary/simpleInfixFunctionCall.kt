infix fun Int.compareTo(other: Int) = 0

fun foo(x: Int) {
    x <caret>compareTo 1
}
