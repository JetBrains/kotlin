// IS_APPLICABLE: false
fun test(x: String?) {
    if (null <caret>!= x) {
        x.length
    }
}
