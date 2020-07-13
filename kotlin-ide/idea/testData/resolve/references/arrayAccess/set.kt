fun a () {
    val list = mutableListOf(1)
    list<caret>[0] = 1
}

// REF: (in kotlin.collections.MutableList).set(kotlin.Int, E)

