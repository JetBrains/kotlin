fun test(): Int {
    try {
        return Klass().getSomething()
    } catch (e: Error) {
        return 1
    }
}
