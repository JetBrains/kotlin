package foo

class A() {
    val a: Int = 1
}

class B() {
    val b: Int = 2
}

fun box(): Boolean {
    return ((A().a == 1) && (B().b == 2));
}