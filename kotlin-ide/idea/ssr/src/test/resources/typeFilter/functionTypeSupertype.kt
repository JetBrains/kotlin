fun myTopFun() {}
fun main() {
    <warning descr="SSR">val v: () -> Unit = ::myTopFun</warning>
    v()
}
class Inheritor : () -> Unit {
    override fun invoke() {}
    <warning descr="SSR">val i = Inheritor()</warning>
}