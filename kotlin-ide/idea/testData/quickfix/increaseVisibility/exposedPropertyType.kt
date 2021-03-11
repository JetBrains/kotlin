// "Make 'Data' public" "true"
// ACTION: Make 'foo' private

private data class Data(val x: Int)

class First {
    val <caret>foo = Data(13)
}
