// WITH_STDLIB

interface Tx {
    suspend fun close()
}

internal suspend inline fun Tx.closeLogging(log: (Exception) -> Unit) {
    try {
        close()
    } catch (e: Exception) {
        log(e)
    }
}

suspend inline fun Tx.closePublic(log: (Exception) -> Unit) {
    close()
}
