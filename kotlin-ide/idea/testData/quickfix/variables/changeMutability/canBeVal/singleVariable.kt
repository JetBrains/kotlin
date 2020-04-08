// "Change to val" "true"
fun foo(p: Int) {
    <caret>var v: Int
    if (p > 0) v = 1 else v = 2
}