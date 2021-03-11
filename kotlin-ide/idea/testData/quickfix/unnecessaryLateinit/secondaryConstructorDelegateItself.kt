// "Remove 'lateinit' modifier" "true"
// ERROR: There's a cycle in the delegation calls chain

class Foo {
    <caret>lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : this(a) {
        bar = "a"
    }
}
