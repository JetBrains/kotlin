fun box(): String {
    foo(false)
    try {
        foo(true)
    } catch (e: Error) {
        return "OK"
    }
    return "FAIL"
}

private fun foo(b: Boolean): Any {
    var result = Any()
    if (b) {
        throw Error()
    }
    return result
}