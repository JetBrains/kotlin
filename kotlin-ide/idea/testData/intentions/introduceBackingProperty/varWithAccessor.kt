class Foo {
    var <caret>x = ""
        get() = field + "!"
        set(value) { field = value + "!" }
}
