// ERROR: Type inference failed: Not enough information to infer parameter E in constructor ArrayList<E : Any!>() Please specify it explicitly.
package demo

internal class Test {
    fun main() {
        val common: List<String?> = ArrayList()
        val raw: List<*> = ArrayList<String?>()
        val superRaw: List<*> = ArrayList<Any?>()
    }
}