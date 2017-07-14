import java.util.ArrayList

internal class A {
    private val list1 = ArrayList<String>()
    private val list2 = ArrayList<String>()
    private val myList3 = ArrayList<String>()
    val list3: List<String>
        get() = myList3

    fun getList1(): List<String> {
        return list1
    }

    fun getList2(): List<String> {
        return list2
    }

    fun foo() {
        list1.add("a")
        list2.add("a")
        myList3.add("a")
    }
}