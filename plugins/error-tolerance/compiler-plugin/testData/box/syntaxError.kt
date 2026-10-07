fun healthy(): String = "OK"

fun broken(): Int {
    return 1 +
}

fun box(): String {
    try {
        broken()
        return "Fail: no error thrown"
    } catch (e: Error) {
        if (!e.message.orEmpty().startsWith("Unresolved compilation problem")) return "Fail: ${e.message}"
    }
    return healthy()
}
