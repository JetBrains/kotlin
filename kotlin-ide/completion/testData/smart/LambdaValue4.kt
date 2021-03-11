fun foo(list: List<String?>, list2: List<Int>) {
    bar(list.map { it?.let { list2.<caret> } })
}

fun bar(p: Collection<Int?>) {
}

// EXIST: size
// EXIST: indexOf
// ABSENT: isEmpty
// ABSENT: toString
