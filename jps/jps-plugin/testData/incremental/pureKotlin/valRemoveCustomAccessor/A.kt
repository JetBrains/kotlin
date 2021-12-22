package test

class A() {
    val x: Int? = 100
        get() = field?.inc()
}
