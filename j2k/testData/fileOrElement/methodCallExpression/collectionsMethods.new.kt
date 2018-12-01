internal class A {
    fun foo(): Map<String?, String?> {
        val list1: List<String?> = emptyList()
        val list2: List<Int?> = listOf(1)
        val set1: Set<String?> = emptySet()
        val set2: Set<String?> = setOf("a")
        return emptyMap()
    }
}