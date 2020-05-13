<warning descr="SSR">fun a() = object {
    val c = 1
}</warning>

class A() {
    private fun b() = object {
        val c = 1
    }

    <warning descr="SSR">fun c() = object {
        val c = 1
    }</warning>

}