package test

fun <T> forEach0(list: List<T>, block: (T) -> Unit) {
    list.forEach(block)
    // Comment after the function
}
