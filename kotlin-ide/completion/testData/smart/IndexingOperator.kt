fun foo(list: List<String>, i: Int) {
    list[<caret>]
}

// EXIST: i
// ABSENT: list
