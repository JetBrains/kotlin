class Wrapper(val f: () -> String)

class Test {
    fun f(): String = "Hello"
    fun reference() = ::f
    val foo = Wrapper(referen<caret>ce())
}