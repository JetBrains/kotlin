inline fun foo1(x: () -> Int = { 88 }) = x()
inline fun foo2() = 88
inline fun foo3() = "foo3 update"
