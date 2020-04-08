fun foo(list: List<String>, intList: MutableList<Int>, stringList: MutableList<String>, p: Any): Collection<Int> {
    val arrayList = arrayListOf<Int>()
    return list.mapTo(<caret>)
}

// EXIST: intList
// EXIST: arrayListOf
// EXIST: stringList
// EXIST: arrayList
// ABSENT: p
