fun foo(): Int {
    val x = 0

    <caret>if (x == 1)
        return x + 1

    return 0
}