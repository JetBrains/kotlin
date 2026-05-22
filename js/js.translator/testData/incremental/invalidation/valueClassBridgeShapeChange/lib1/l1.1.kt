value class Token(val raw: Int)

fun makeToken(x: Int): Token = Token(x + 1)

fun readToken(token: Token): Int = token.raw
