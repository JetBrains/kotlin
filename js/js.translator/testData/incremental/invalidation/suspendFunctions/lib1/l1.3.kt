suspend fun fooX() = 11
suspend inline fun fooY() = 22

object Test {
    suspend fun complex(): Int = fooX() + fooX()
}