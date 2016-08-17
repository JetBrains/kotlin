class null_comparison_1_class

fun null_comparison_1_getClass(): null_comparison_1_class? {
    return null
}

fun null_comparison_1(): Int {
    val a: null_comparison_1_class? = null_comparison_1_getClass()
    println(if (a == null) 555 else 9990)
    if (a == null) {
        return 87
    }
    return 945
}