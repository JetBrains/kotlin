class SomeClass<K, V>
typealias TaSomeClass<K, V> = SomeClass<K, V>

fun usesSomeClass(p: SomeClass<*, *>) {

}


fun usage() {
    usesSomeClass(<caret>)
}

// EXIST: {"lookupString":"TaSomeClass","tailText":"() (<root>)","typeText":"SomeClass<K, V>"}