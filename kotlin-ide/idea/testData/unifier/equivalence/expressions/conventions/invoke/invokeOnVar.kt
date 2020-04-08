// DISABLE-ERRORS
fun Int.invoke() = this + 1

val x = 1
val a = <selection>x()</selection>
val b = x.invoke()
val c = invoke(x)
val d = x.plus()