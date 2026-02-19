fun qux(): Int {
    try {
        return foo()
    } catch(ex: ClassCastException) {
        return 2
    }
}
