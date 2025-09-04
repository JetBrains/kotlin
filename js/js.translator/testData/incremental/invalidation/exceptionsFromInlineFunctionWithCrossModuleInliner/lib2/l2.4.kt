fun qux(): Int {
    try {
        return foo()
    } catch(ex: NullPointerException) {
        return 4
    }
}
