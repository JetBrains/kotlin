internal class A {
    fun foo(): Map<String, String> {
        val list1 = emptyList<String>()
        val list2 = listOf(1)
        val set1 = emptySet<String>()
        val set2 = setOf("a")
        return emptyMap()
    }
}