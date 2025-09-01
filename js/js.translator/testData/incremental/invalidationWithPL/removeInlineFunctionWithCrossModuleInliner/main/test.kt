fun test(): Int {
    try {
        return inlineFoo { foo() }
    } catch (e: Error) {
        return 1
    }
}
