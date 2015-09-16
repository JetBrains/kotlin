import java.util.ArrayList

internal class C {
    fun foo(list: MutableList<String>) {
        for (i in list.indices) {
            list.set(i, "a")
        }
    }
}
