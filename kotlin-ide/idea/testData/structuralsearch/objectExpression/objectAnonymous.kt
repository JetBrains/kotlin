fun a() = object {
    val c = 1
}

class A() {
    <warning descr="SSR">private fun b() = object {
        val c = 1
    }</warning>

    fun c() = object {
        val c = 1
    }
}