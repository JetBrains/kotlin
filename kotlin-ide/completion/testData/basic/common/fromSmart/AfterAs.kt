fun foo(list: List<String>){}

fun bar(o: Any) {
    foo(o as <caret>)
}

// EXIST: { itemText: "List<String>" }
