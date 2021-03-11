val x = foo.bar {
    it + 2
}.let {
    println(it)
}

// SET_FALSE: CONTINUATION_INDENT_FOR_CHAINED_CALLS
