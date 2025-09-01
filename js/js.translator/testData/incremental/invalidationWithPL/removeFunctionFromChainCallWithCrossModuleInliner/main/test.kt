fun test(): Int {
    try {
        return makeKlass().foo()
    } catch (e: Error) {
        return 1
    }
}
