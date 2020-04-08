import java.util.*

internal class Iterator {

    var mutableMap1: MutableMap<String, String> = HashMap()
    var mutableMap2: MutableMap<String, String> = HashMap()

    fun testFields() {
        mutableMap1.values.add("")
        mutableMap2.entries.iterator().remove()
    }

    fun testFunctionParameters(immutableCollection: Collection<String>, mutableList: MutableList<Int>) {
        val it = immutableCollection.iterator()
        while (it.hasNext()) {
            it.next()
        }
        mutableList.listIterator().add(2)
    }
}