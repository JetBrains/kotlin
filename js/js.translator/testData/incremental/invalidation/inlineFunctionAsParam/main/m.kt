
inline fun test1(x: () -> Any) = x()
fun test2(x: () -> Any) = x()
inline fun test3(x: () -> Any) = x()

fun box() = test1(::foo1).toString() + test2(::foo2).toString() + test3 { foo3() }.toString()
