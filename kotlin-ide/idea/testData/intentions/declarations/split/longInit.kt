fun foo(n: Int) {
    <caret>val x =
        if (n > 0)
            "> 0"
        else
            "<= 0"
}