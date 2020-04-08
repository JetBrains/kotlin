class SomeClass
typealias TaSomeClass = SomeClass

fun usesSomeClass(p: SomeClass) {

}


fun usage() {
    usesSomeClass(<caret>)
}

// EXIST: {"lookupString":"TaSomeClass","tailText":"() (<root>)"}