fun box(): String = run {
    @Suppress("DEPRECATION_ERROR")
    val golden = immutableBlobOf(123)
    if (golden[0] == 123.toByte()) {
        return "OK"
    }
    return "FAIL: ${golden[0]}"
}