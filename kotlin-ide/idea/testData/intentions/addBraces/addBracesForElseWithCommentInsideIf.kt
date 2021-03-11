fun test(x: Int, b: Boolean) {
    val foo = if (x == 1)
        if (b) 1 else<caret> 2 // comment
    else 0
}