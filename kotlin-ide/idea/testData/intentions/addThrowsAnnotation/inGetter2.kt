// WITH_RUNTIME

@get:Throws(RuntimeException::class)
val a: String
    get() = <caret>throw Exception()