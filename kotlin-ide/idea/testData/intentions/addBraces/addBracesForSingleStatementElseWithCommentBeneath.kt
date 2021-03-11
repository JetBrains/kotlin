fun test(b: Boolean) {
    if (b) foo(0)
    else<caret>
        while (true) {
            foo(1)
        }
    // comment about call below
}

fun foo(i: Int) {}