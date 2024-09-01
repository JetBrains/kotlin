package foo

class A() {
    val p = { "OK" }
}


fun box(): String {
    return A().p()
}