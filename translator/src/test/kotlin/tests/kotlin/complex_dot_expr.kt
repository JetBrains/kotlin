
class MyClass(val i: Int)

fun gen(i: Int) = MyClass(i)
fun test1(q: Int) = gen(q).i
fun test2(w: Int) = MyClass(w).i

