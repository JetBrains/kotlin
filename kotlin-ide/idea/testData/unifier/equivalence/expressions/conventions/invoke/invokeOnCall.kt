// DISABLE-ERRORS
fun f(n: Int): Int = n
fun Int.invoke() = this + 1

val a = <selection>f(1)()</selection>
val b = f(1).invoke()
val c = invoke(f(1))
val d = f(1).plus()