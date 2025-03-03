fun box() = expectThrowableMessage {
    val text: String? = "Hello"
		assert(	text	!=	null	&&	text	.length			==	5	&&	text	.lowercase()	==	text)
}
