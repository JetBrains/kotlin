package test

actual object Obj

fun foo(o: Obj) {
    o.hashCode()
}