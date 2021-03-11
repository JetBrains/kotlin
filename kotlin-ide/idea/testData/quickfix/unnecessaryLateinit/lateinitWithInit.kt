// "Remove 'lateinit' modifier" "true"

class Foo {
    <caret>lateinit var bar: String

    init {
        bar = ""
    }
}