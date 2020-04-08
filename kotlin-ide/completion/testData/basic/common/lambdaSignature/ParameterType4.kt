fun bar() {
    val handler = { i: Map<String, <caret> }
}

// EXIST: Int
// EXIST: String
// ABSENT: bar
// ABSENT: handler