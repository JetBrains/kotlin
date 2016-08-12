external fun assert_c(value: Boolean)

fun assert(value: Boolean) {
    println(value)
    assert_c(value)
}