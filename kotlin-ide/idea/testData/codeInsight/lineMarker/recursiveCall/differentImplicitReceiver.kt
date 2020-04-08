fun Any.foo() {
    with("different extension receiver") {
        <lineMarker>foo</lineMarker>()
    }
}


class Klass(p: String) {
    private fun bar() {
        with(Klass("different dispatch receiver")) {
            bar()
        }
    }
}
