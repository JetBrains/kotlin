
class MyException : Exception

@Throws(java.io.IOException::class, MyException::class)
fun readFile(name: String): String {}

// Should be mapped to java.lang.Throwable
@Throws(kotlin.Throwable::class)
fun baz() {}
