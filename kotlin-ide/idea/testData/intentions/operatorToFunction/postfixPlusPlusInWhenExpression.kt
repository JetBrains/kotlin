fun foo() {
    var a = 0
    when {
        true -> a++<caret>
        else -> a--
    }
}
