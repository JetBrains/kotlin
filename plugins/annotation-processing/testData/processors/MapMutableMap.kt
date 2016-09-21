annotation class Anno

interface Intf

@Anno
class Test {
    fun a(map: Map<Int, Intf>) {}
    fun b(map: MutableMap<Int, Intf>) {}

    fun c(map: Map<Int, Test>) {}
    fun d(map: MutableMap<Int, Test>) {}
}