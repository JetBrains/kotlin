package foo

class A() {
    val p = { true }
}


fun box(): Boolean {
    return A().p()
}