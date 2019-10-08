import java.util.ArrayList

class Test {
    internal var list: List<MutableList<Int>> = ArrayList()
    fun test() {
        list[0].add(1)
    }
}