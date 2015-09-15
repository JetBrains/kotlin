import java.util.ArrayList

internal class C {
    internal fun foo1(list: MutableList<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }

    internal fun foo2(list: ArrayList<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }
}