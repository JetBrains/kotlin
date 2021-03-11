// IS_APPLICABLE: false
fun testing(x: Int, y: Int, f: (a: Int, b: Int) -> Int): Int {
    return f(x, y)
}

fun main() {
    val num = testing(1, 2, {<caret> x, y -> x + y })
}
