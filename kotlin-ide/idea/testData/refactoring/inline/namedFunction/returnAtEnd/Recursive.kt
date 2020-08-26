fun fact(x: Int): Int {
    return if (x < 2) 1 else <caret>fact(x - 1)
}