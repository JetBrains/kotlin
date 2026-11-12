@JvmInline
value class PostRequestUri(val uri: String) {
    override fun toString() = uri
}

@JvmInline
value class CardOrNoteId(val id: Long) {
    override fun toString(): String = id.toString()

    fun describe(prefix: String): String = prefix + id
}
