fun test(): Int {
    try {
        return foo().toInt() + 1
    } catch (e: Error) {
        return 6
    }
}
