class IOException : Throwable()

interface JsonWriter {
    @Throws(IOException::class)
    fun value(value: Int): JsonWriter

    @Throws(IOException::class)
    fun value(value: Long): JsonWriter
}