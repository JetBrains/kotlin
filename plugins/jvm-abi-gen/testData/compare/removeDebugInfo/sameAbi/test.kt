package test

inline fun <T> forEach0(list: List<T>, block: (T) -> Unit): List<T> {
    val list2 = list

    list2.forEach(block)





    return list2
}
