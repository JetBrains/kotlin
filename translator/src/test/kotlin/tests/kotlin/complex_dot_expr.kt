
class complex_dot_expr_MyClass(val i: Int)

fun gen(i: Int) = complex_dot_expr_MyClass(i)
fun test1(q: Int) = gen(q).i
fun test2(w: Int) = complex_dot_expr_MyClass(w).i

