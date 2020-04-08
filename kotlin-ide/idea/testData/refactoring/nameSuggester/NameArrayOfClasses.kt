class A() {}

fun <T> array(vararg t : T) : Array<T> = t as Array<T>

fun a() {
    <selection>array(A(), A())</selection>
}
/*
array
arrayOfAs
*/