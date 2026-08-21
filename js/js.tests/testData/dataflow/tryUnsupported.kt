// Soundness: try/catch is unsupported — CFG marks unsupportedConstruct.

fun box(): String {
    return try {
        "OK"
    } catch (e: Throwable) {
        "FAIL"
    }
}
