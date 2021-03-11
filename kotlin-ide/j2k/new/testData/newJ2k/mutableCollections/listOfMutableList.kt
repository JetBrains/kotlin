import java.util.ArrayList

class Test {
    var list: List<MutableList<Int>> = ArrayList()
    fun test() {
        list[0].add(1)
    }
}