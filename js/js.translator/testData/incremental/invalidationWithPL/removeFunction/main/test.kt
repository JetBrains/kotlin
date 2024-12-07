fun test(): Int {
    try {
        return foo().toInt()
    } catch (e: Error) {
        return 1
    }
}
