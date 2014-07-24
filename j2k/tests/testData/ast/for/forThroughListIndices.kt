import java.util.ArrayList

class C {
    fun foo1(list: List<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }

    fun foo2(list: ArrayList<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }
}