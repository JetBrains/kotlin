class Test {
    var test: Int
        get() {
            return 0
        }
        set(value) {
            throw NotSupportedException()
        }
}