internal class C {
    fun foo(list: MutableList<String?>) {
        for (i in list.indices) {
            list[i] = "a"
        }
    }
}
