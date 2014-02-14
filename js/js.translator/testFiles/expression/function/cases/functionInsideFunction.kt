package foo


fun box(): Boolean {
    fun f() = 3

    return (((f() + f()) == 6) && (b() == 24))
}


fun b(): Int {

    fun a(): Int {
        fun c() = 4
        return c() * 3
    }
    val a = 2
    return a() * a
}