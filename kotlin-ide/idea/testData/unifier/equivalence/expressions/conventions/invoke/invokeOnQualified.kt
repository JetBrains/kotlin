// DISABLE-ERRORS
class X(val n: Int)
fun Int.invoke() = this + 1

val x = X(1)
val a = <selection>x.n()</selection>
val b = x.n.invoke()
val c = invoke(x.n)
val d = x.n.plus()