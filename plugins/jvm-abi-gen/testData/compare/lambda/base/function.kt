package test

fun function(): Int {
    val square: (Int) -> Int = { it * it }
    return square(2)
}