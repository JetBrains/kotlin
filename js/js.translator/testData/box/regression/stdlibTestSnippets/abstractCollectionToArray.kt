// EXPECTED_REACHABLE_NODES: 1700
// KJS_WITH_FULL_RUNTIME

fun abstractCollectionToArray() {
    class TestCollection<out E>(val data: Collection<E>) : AbstractCollection<E>() {
        val invocations = mutableListOf<String>()
        override val size get() = data.size
        override fun iterator() = data.iterator()

        override fun toArray(): Array<Any?> {
            invocations += "toArray1"
            return data.toTypedArray()
        }
        public override fun <T> toArray(array: Array<T>): Array<T> {
            invocations += "toArray2"
            return super.toArray(array)
        }
    }
    val data = listOf("abc", "def")
    val coll = TestCollection(data)

    val arr1 = coll.toTypedArray()
    assertEquals(data, arr1.asList())
    assertTrue("toArray1" in coll.invocations || "toArray2" in coll.invocations)

    val arr2: Array<String> = coll.toArray(Array(coll.size + 1) { "" })
    assertEquals(data + listOf(""), arr2.asList())
}

fun box(): String {
    abstractCollectionToArray()
    return "OK"
}