fun test(): Int {
    try {
        return foo() ?: bar()
    } catch (e: Error) {
        return 1
    }
}
