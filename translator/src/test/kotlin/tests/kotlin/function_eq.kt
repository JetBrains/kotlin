
class MyClass(val i: Int)

fun inc(i: Int) = i + 1
fun dinc(i: Int) = inc(inc(i))
fun sum(i: Int, j: Int) = i + j
fun gen() = MyClass(1)

