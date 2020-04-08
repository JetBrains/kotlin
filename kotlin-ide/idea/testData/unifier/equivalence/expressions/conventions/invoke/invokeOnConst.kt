// DISABLE-ERRORS
fun Int.invoke() = this + 1

val a = <selection>1()</selection>
val b = 1.invoke()
val c = invoke(1)
val d = 1.plus()