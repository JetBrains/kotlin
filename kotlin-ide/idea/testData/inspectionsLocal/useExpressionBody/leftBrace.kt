// HIGHLIGHT: INFORMATION
// PROBLEM: Use expression body instead of return

fun simple(): Int {<caret>
    return 1 *
           (2 + 3)
}