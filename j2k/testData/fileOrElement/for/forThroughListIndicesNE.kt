import java.util.ArrayList

class C {
    fun foo(list: MutableList<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }
}
