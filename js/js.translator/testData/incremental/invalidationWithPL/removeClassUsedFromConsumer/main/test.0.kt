import sample.Foo

fun test(): Int {
    return try {
        Foo().value()
    } catch (e: Error) {
        1
    }
}
