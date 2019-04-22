import java.util.*

internal class A {
    fun foo(): Map<String, String> {
        val list1: List<String> = emptyList()
        val list2 = listOf(1)
        val set1: Set<String> = emptySet()
        val set2 = setOf("a")
        return emptyMap()
    }
}