fun a() {
    when (val b = 5) {
        <caret>1, 2,
        3, ->
    }
}