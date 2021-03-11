fun test() {
    (fu<caret>n(action: (Int) -> Unit) {
        action(42)
    })(::println)
}