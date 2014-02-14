package foo


class A() {
    private var c: Int = 3
        private get
        private set

    fun f() = c + 1
}

fun box(): Boolean {
    return A().f() == 4
}