package test

inline fun <T> forEach0(list: List<T>, block: (T) -> Unit): List<T> {
    val list1 = list
    list1.forEach(block)
    return list1
}
