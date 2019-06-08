import java.util.ArrayList

class TestMutableCollection {
    val list: MutableList<String> = ArrayList()

    fun test() {
        val it = list.iterator()
        while (it.hasNext()) {
            val s = it.next()
            if (s == "") it.remove()
        }
    }
}