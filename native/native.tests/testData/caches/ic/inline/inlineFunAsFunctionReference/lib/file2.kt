package test

fun bar(): Int {
    val ref: () -> Int = ::foo
    return ref()
}
