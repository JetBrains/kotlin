package foo

class A() {
    val x = 4
}

fun box(): Boolean {
    var a: A? = null;
    return (a?.x == null);
}