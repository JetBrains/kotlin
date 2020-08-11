class Wrapper(val f: () -> Test)
class Test {
    fun f(): String = "Hello"
    val reference = ::Test
    val foo = Wrapper(refere<caret>nce)
}