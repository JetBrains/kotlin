class Wrapper<T>(val value: T)

fun test() {
    listOf<String>().map(::Wrap<caret>)
}

// EXIST: { lookupString: "Wrapper", itemText: "Wrapper",    tailText: "(value: T)", attributes: "" }
// NOTHING_ELSE