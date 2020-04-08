fun a() {
    when (val b = 5) {
        1, 2,
        <caret>3,
        ->
    }
}