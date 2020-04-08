fun foo(list: List<String>): Collection<Int> {
    return list.map { it.<caret> }
}

// EXIST: length
// ABSENT: isEmpty
