class function_eq_MyClass(val i: Int)

fun function_eq_inc(i: Int) = i + 1
fun function_eq_dinc(i: Int) = function_eq_inc(function_eq_inc(i))
fun function_eq_sum(i: Int, j: Int) = i + j
fun function_eq_gen() = function_eq_MyClass(1)

