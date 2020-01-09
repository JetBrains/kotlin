class Foo {
    private var value: String? = null
        private get() {
            if (true) {
                field = "new"
            }
            return field
        }
}
