fun bar() {
    val handler = { i: Int, list: List<String>, s: <caret> }
}

// EXIST: Int
// EXIST: String
// ABSENT: bar
// ABSENT: handler