// "Remove 'lateinit' modifier" "true"

class Foo {
    <caret>lateinit var bar: String

    constructor(baz: Int) {
        bar = ""
    }
}
