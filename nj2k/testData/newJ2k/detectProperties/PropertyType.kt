import java.util.ArrayList

internal class A {
    private val list1 = ArrayList<String>()
    private val list2: MutableList<String> = ArrayList()
    private val myList3: MutableList<String> = ArrayList()
    fun getList1(): List<String> {
        return list1
    }

    fun getList2(): List<String> {
        return list2
    }

    val list3: List<String>
        get() = myList3

    fun foo() {
        list1.add("a")
        list2.add("a")
        myList3.add("a")
    }
}