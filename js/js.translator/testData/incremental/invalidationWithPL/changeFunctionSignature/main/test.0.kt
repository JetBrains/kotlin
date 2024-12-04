fun test(): Int {
    try {
        return foo()
    } catch (e: Error) {
        return 1
    }
}
