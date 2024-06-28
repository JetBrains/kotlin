fun test(): Int {
    try {
        return inlineFun()
    } catch (e: Error) {
        return 1
    }
}
