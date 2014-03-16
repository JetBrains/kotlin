package foo

class Test() {
    val p = true
}

fun box(): Boolean {
    return Test().p
}