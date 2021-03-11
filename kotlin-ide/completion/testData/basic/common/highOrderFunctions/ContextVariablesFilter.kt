fun test(p1: (String) -> Boolean, p2: (Int) -> Boolean) {
    listOf("a", "b").filt<caret>
}

// EXIST: { itemText: "filter", tailText: " {...} (predicate: (String) -> Boolean) for Iterable<T> in kotlin.collections", typeText:"List<String>" }
// EXIST: { itemText: "filter", tailText: "(p1) for Iterable<T> in kotlin.collections", typeText: "List<String>" }
// ABSENT: { itemText: "filter", tailText: "(p2) for Iterable<T> in kotlin.collections", typeText: "List<String>" }
