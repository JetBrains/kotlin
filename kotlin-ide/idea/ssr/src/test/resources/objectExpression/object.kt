<warning descr="SSR">fun a() = object {
    val c = 1
}</warning>

class A() {
    <warning descr="SSR">private fun b() = object {
        val c = 1
    }</warning>

    <warning descr="SSR">fun c() = object {
        val c = 1
    }</warning>

}