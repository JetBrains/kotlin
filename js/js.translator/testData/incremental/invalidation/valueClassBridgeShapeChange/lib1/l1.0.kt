value class Token(val raw: Int)

fun makeToken(x: Int): Token? = Token(x)

fun readToken(token: Token?): Int = token?.raw ?: -1
