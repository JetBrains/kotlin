// WITH_RUNTIME

@set:Throws(RuntimeException::class)
var setter: String = ""
    set(value) = <caret>throw Exception()