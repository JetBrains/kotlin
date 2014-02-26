package foo

class A() {
    fun doSomething() {
    }
}

fun box(): Boolean {
    var a: A? = null;
    a?.doSomething()
    return true;
}